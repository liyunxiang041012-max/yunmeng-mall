package com.liyun.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.po.FlashSale;
import com.liyun.promotion.domain.po.FlashSaleItem;
import com.liyun.promotion.exception.BadRequestException;
import com.liyun.promotion.feign.ItemFeign;
import com.liyun.promotion.mapper.FlashSaleItemMapper;
import com.liyun.promotion.mapper.FlashSaleMapper;
import com.liyun.promotion.service.IFlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl extends ServiceImpl<FlashSaleMapper, FlashSale> implements IFlashSaleService {

    private final FlashSaleItemMapper itemMapper;
    private final FlashSaleMapper flashSaleMapper;
    private final RedissonClient redissonClient;
    private final ItemFeign itemFeign;

    /** 最低折扣率：至少20% off，即秒杀价 <= 原价 * 0.8 */
    private static final double MIN_DISCOUNT_RATE = 0.8;

    /** 预热天数：只预热3天内的商品 */
    private static final int WARMING_DAYS = 3;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 管理员 ====================

    @Override
    @Transactional
    public void createFlashSale(FlashSale flashSale, List<FlashSaleItem> items) {
        flashSale.setStatus(1);
        flashSale.setCreateTime(LocalDateTime.now());
        flashSale.setUpdateTime(LocalDateTime.now());
        save(flashSale);

        for (FlashSaleItem item : items) {
            validateDiscount(item);
            item.setFlashSaleId(flashSale.getId());
            item.setSold(0);
            itemMapper.insert(item);
        }
    }

    // ==================== 用户端 ====================

    @Override
    public List<Map<String, Object>> getOngoingFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        // 查询所有进行中的秒杀商品（start_time <= now <= end_time）
        List<FlashSaleItem> ongoingItems = itemMapper.selectList(
                new LambdaQueryWrapper<FlashSaleItem>()
                        .le(FlashSaleItem::getStartTime, now)
                        .ge(FlashSaleItem::getEndTime, now)
                        .orderByAsc(FlashSaleItem::getStartTime));

        if (ongoingItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 按活动分组
        return groupItemsBySale(ongoingItems, "ongoing");
    }

    @Override
    public List<Map<String, Object>> getWarmingFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warmingEnd = now.plusDays(WARMING_DAYS);
        // 查询预热中的商品（now < start_time <= now + 3天）
        List<FlashSaleItem> warmingItems = itemMapper.selectList(
                new LambdaQueryWrapper<FlashSaleItem>()
                        .gt(FlashSaleItem::getStartTime, now)
                        .le(FlashSaleItem::getStartTime, warmingEnd)
                        .orderByAsc(FlashSaleItem::getStartTime));

        if (warmingItems.isEmpty()) {
            return Collections.emptyList();
        }

        return groupItemsBySale(warmingItems, "warming");
    }

    @Override
    public Map<String, Object> getFlashSaleItemDetail(Long itemId) {
        FlashSaleItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BadRequestException("秒杀商品不存在");

        FlashSale sale = getById(item.getFlashSaleId());
        LocalDateTime now = LocalDateTime.now();

        // 计算商品状态
        String itemStatus;
        if (now.isBefore(item.getStartTime())) {
            itemStatus = "warming";  // 预热中
        } else if (now.isAfter(item.getEndTime())) {
            itemStatus = "ended";    // 已结束
        } else {
            itemStatus = "ongoing";  // 进行中
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("flashSaleId", item.getFlashSaleId());
        map.put("flashSaleName", sale != null ? sale.getName() : "");
        map.put("skuId", item.getSkuId());
        map.put("spuId", item.getSpuId());
        map.put("shopId", item.getShopId());
        map.put("flashPrice", item.getFlashPrice());
        map.put("stock", item.getStock());
        map.put("sold", item.getSold());
        map.put("remaining", item.getStock() - item.getSold());
        map.put("limitPerUser", item.getLimitPerUser());
        map.put("startTime", item.getStartTime());
        map.put("endTime", item.getEndTime());
        map.put("status", itemStatus);
        map.put("countdownSeconds", itemStatus.equals("warming")
                ? java.time.Duration.between(now, item.getStartTime()).getSeconds()
                : (itemStatus.equals("ongoing")
                    ? java.time.Duration.between(now, item.getEndTime()).getSeconds()
                    : 0));
        return map;
    }

    @Override
    @Transactional
    public void placeFlashOrder(Long itemId, Long userId) {
        FlashSaleItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BadRequestException("秒杀商品不存在");

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            throw new BadRequestException("秒杀尚未开始，请等待开始时间");
        }
        if (now.isAfter(item.getEndTime())) {
            throw new BadRequestException("秒杀已结束");
        }

        String lockKey = "lock:flash:" + itemId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            int result = flashSaleMapper.deductStock(itemId);
            if (result <= 0) {
                throw new BadRequestException("秒杀库存不足");
            }
            log.info("[FLASH] 秒杀下单成功: itemId={}, userId={}", itemId, userId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 商家端 ====================

    @Override
    @Transactional
    public void merchantSubmitItems(List<FlashSaleItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("请至少提交一件商品");
        }

        Long userId = UserContext.getUserId();
        // 自动关联平台常驻秒杀活动
        Long flashSaleId = getOrCreateDefaultFlashSale();

        for (FlashSaleItem item : items) {
            // 1. 校验时间
            if (item.getStartTime() == null || item.getEndTime() == null) {
                throw new BadRequestException("秒杀开始时间和结束时间不能为空");
            }
            if (!item.getStartTime().isAfter(LocalDateTime.now())) {
                throw new BadRequestException("秒杀开始时间必须晚于当前时间");
            }
            if (!item.getEndTime().isAfter(item.getStartTime())) {
                throw new BadRequestException("秒杀结束时间必须晚于开始时间");
            }

            // 2. 校验折扣 >= 20%
            validateDiscount(item);

            // 3. 设置关联信息
            item.setFlashSaleId(flashSaleId);
            item.setShopId(userId);
            item.setSold(0);
            itemMapper.insert(item);
        }
        log.info("[FLASH-MERCHANT] 商家userId={} 提交{}件商品参与秒杀", userId, items.size());
    }

    @Override
    @Transactional
    public void merchantUpdateTime(Long itemId, String startTimeStr, String endTimeStr) {
        FlashSaleItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BadRequestException("秒杀商品不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        // 只有未开始的秒杀商品才能调整时间
        if (!item.getStartTime().isAfter(now)) {
            throw new BadRequestException("秒杀已开始或已结束，无法调整时间");
        }

        LocalDateTime newStart = LocalDateTime.parse(startTimeStr, FMT);
        LocalDateTime newEnd = LocalDateTime.parse(endTimeStr, FMT);

        if (!newStart.isAfter(now)) {
            throw new BadRequestException("新的开始时间必须晚于当前时间");
        }
        if (!newEnd.isAfter(newStart)) {
            throw new BadRequestException("新的结束时间必须晚于开始时间");
        }

        item.setStartTime(newStart);
        item.setEndTime(newEnd);
        itemMapper.updateById(item);
        log.info("[FLASH-MERCHANT] 调整秒杀商品时间: itemId={}, {} ~ {}", itemId, newStart, newEnd);
    }

    @Override
    public List<Map<String, Object>> getMerchantDashboard(Long shopId) {
        // 查询该商家所有秒杀商品
        List<FlashSaleItem> merchantItems = itemMapper.selectList(
                new LambdaQueryWrapper<FlashSaleItem>()
                        .eq(FlashSaleItem::getShopId, shopId)
                        .orderByDesc(FlashSaleItem::getStartTime));

        if (merchantItems.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (FlashSaleItem item : merchantItems) {
            FlashSale sale = getById(item.getFlashSaleId());

            // 计算商品状态
            String status;
            if (now.isBefore(item.getStartTime())) {
                status = "warming";
            } else if (now.isAfter(item.getEndTime())) {
                status = "ended";
            } else {
                status = "ongoing";
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("flashSaleId", item.getFlashSaleId());
            map.put("flashSaleName", sale != null ? sale.getName() : "");
            map.put("skuId", item.getSkuId());
            map.put("spuId", item.getSpuId());
            map.put("flashPrice", item.getFlashPrice());
            map.put("stock", item.getStock());
            map.put("sold", item.getSold());
            map.put("remaining", item.getStock() - item.getSold());
            map.put("limitPerUser", item.getLimitPerUser());
            map.put("startTime", item.getStartTime());
            map.put("endTime", item.getEndTime());
            map.put("status", status);
            // 是否可调整时间
            map.put("canAdjustTime", status.equals("warming"));
            // 销售额（分）
            map.put("totalRevenue", (long) item.getSold() * item.getFlashPrice());
            result.add(map);
        }
        return result;
    }

    @Override
    public PageDTO<Map<String, Object>> merchantQueryFlashSaleItems(Long shopId, Integer pageNo, Integer pageSize, String status) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<FlashSaleItem> wrapper = new LambdaQueryWrapper<FlashSaleItem>()
                .eq(FlashSaleItem::getShopId, shopId);

        if ("warming".equals(status)) {
            wrapper.gt(FlashSaleItem::getStartTime, now);
        } else if ("ongoing".equals(status)) {
            wrapper.le(FlashSaleItem::getStartTime, now).ge(FlashSaleItem::getEndTime, now);
        } else if ("ended".equals(status)) {
            wrapper.lt(FlashSaleItem::getEndTime, now);
        }
        wrapper.orderByDesc(FlashSaleItem::getStartTime);

        Page<FlashSaleItem> page = new Page<>(pageNo, pageSize);
        Page<FlashSaleItem> result = itemMapper.selectPage(page, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (FlashSaleItem item : result.getRecords()) {
            FlashSale sale = getById(item.getFlashSaleId());
            String itemStatus;
            if (now.isBefore(item.getStartTime())) {
                itemStatus = "warming";
            } else if (now.isAfter(item.getEndTime())) {
                itemStatus = "ended";
            } else {
                itemStatus = "ongoing";
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("flashSaleId", item.getFlashSaleId());
            map.put("flashSaleName", sale != null ? sale.getName() : "");
            map.put("skuId", item.getSkuId());
            map.put("spuId", item.getSpuId());
            map.put("flashPrice", item.getFlashPrice());
            map.put("stock", item.getStock());
            map.put("sold", item.getSold());
            map.put("remaining", item.getStock() - item.getSold());
            map.put("limitPerUser", item.getLimitPerUser());
            map.put("startTime", item.getStartTime());
            map.put("endTime", item.getEndTime());
            map.put("status", itemStatus);
            map.put("canAdjustTime", "warming".equals(itemStatus));
            map.put("totalRevenue", (long) item.getSold() * item.getFlashPrice());
            records.add(map);
        }
        return new PageDTO<>(result.getTotal(), result.getPages(), records);
    }

    // ==================== 内部方法 ====================

    /**
     * 获取或创建平台常驻秒杀活动（自动维护，商家无需选择活动）
     * 活动名称为"平台限时秒杀"，状态始终为进行中
     */
    private Long getOrCreateDefaultFlashSale() {
        FlashSale existing = lambdaQuery()
                .eq(FlashSale::getName, "平台限时秒杀")
                .last("LIMIT 1")
                .one();
        if (existing != null) {
            return existing.getId();
        }
        // 自动创建常驻活动
        FlashSale fs = new FlashSale();
        fs.setId(1L);
        fs.setName("平台限时秒杀");
        fs.setStartTime(LocalDateTime.now());
        fs.setEndTime(LocalDateTime.now().plusYears(10));
        fs.setStatus(2); // 始终进行中
        fs.setCreateTime(LocalDateTime.now());
        fs.setUpdateTime(LocalDateTime.now());
        save(fs);
        log.info("[FLASH] 自动创建平台常驻秒杀活动: id={}", fs.getId());
        return fs.getId();
    }

    /**
     * 按活动分组秒杀商品，附带活动信息和倒计时
     */
    private List<Map<String, Object>> groupItemsBySale(List<FlashSaleItem> items, String type) {
        Map<Long, List<FlashSaleItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(FlashSaleItem::getFlashSaleId));

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<Long, List<FlashSaleItem>> entry : grouped.entrySet()) {
            FlashSale sale = getById(entry.getKey());
            if (sale == null) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("flashSaleId", sale.getId());
            map.put("flashSaleName", sale.getName());

            List<Map<String, Object>> itemList = new ArrayList<>();
            for (FlashSaleItem item : entry.getValue()) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("id", item.getId());
                im.put("skuId", item.getSkuId());
                im.put("spuId", item.getSpuId());
                im.put("shopId", item.getShopId());
                im.put("flashPrice", item.getFlashPrice());
                im.put("stock", item.getStock());
                im.put("sold", item.getSold());
                im.put("remaining", item.getStock() - item.getSold());
                im.put("limitPerUser", item.getLimitPerUser());
                im.put("startTime", item.getStartTime());
                im.put("endTime", item.getEndTime());

                // 倒计时（秒）
                if ("warming".equals(type)) {
                    im.put("countdownSeconds", java.time.Duration.between(now, item.getStartTime()).getSeconds());
                } else {
                    im.put("countdownSeconds", java.time.Duration.between(now, item.getEndTime()).getSeconds());
                }
                itemList.add(im);
            }
            map.put("items", itemList);
            result.add(map);
        }
        return result;
    }

    /**
     * 校验秒杀折扣：秒杀价必须 <= 原价 * 0.8（至少优惠20%）
     */
    private void validateDiscount(FlashSaleItem item) {
        if (item.getSkuId() == null || item.getFlashPrice() == null) {
            throw new BadRequestException("SKU ID和秒杀价格不能为空");
        }
        try {
            Map<String, Object> response = itemFeign.getSkuInfo(item.getSkuId());
            if (response == null) {
                throw new BadRequestException("SKU信息查询失败: skuId=" + item.getSkuId());
            }
            // Feign返回的是 Result<SkuInfoDTO>，实际数据在 data 字段
            @SuppressWarnings("unchecked")
            Map<String, Object> skuInfo = (Map<String, Object>) response.get("data");
            if (skuInfo == null || skuInfo.get("price") == null) {
                throw new BadRequestException("SKU不存在或价格信息缺失: skuId=" + item.getSkuId());
            }
            Long originalPrice = Long.valueOf(String.valueOf(skuInfo.get("price")));
            if (originalPrice <= 0) {
                throw new BadRequestException("SKU原价异常: skuId=" + item.getSkuId());
            }

            long maxFlashPrice = (long) (originalPrice * MIN_DISCOUNT_RATE);
            if (item.getFlashPrice() > maxFlashPrice) {
                double actualDiscount = (double) item.getFlashPrice() / originalPrice * 100;
                throw new BadRequestException(
                        String.format("折扣不足！原价%.2f元，秒杀价%.2f元，当前折扣%.1f%%，要求至少优惠20%%（秒杀价不超过%.2f元）",
                                originalPrice / 100.0, item.getFlashPrice() / 100.0,
                                actualDiscount, maxFlashPrice / 100.0));
            }
            log.debug("[FLASH] 折扣校验通过: skuId={}, 原价={}, 秒杀价={}",
                    item.getSkuId(), originalPrice, item.getFlashPrice());
        } catch (Exception e) {
            if (e instanceof BadRequestException) throw e;
            log.warn("[FLASH] 折扣校验失败(Feign调用异常): skuId={}, error={}", item.getSkuId(), e.getMessage());
            throw new BadRequestException("无法获取SKU原价进行折扣校验，请稍后重试");
        }
    }
}



