package com.liyun.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.common.context.UserContext;
import com.liyun.promotion.domain.po.FlashSale;
import com.liyun.promotion.domain.po.FlashSaleItem;
import com.liyun.promotion.exception.BadRequestException;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl extends ServiceImpl<FlashSaleMapper, FlashSale> implements IFlashSaleService {

    private final FlashSaleItemMapper itemMapper;
    private final FlashSaleMapper flashSaleMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public void createFlashSale(FlashSale flashSale, List<FlashSaleItem> items) {
        flashSale.setStatus(1);
        flashSale.setCreateTime(LocalDateTime.now());
        flashSale.setUpdateTime(LocalDateTime.now());
        save(flashSale);

        for (FlashSaleItem item : items) {
            item.setFlashSaleId(flashSale.getId());
            item.setSold(0);
            itemMapper.insert(item);
        }
    }

    @Override
    public List<Map<String, Object>> getCurrentFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> sales = lambdaQuery()
                .le(FlashSale::getStartTime, now)
                .ge(FlashSale::getEndTime, now)
                .eq(FlashSale::getStatus, 2)
                .orderByAsc(FlashSale::getStartTime)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (FlashSale sale : sales) {
            List<FlashSaleItem> items = itemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FlashSaleItem>()
                            .eq(FlashSaleItem::getFlashSaleId, sale.getId())
                            .orderByAsc(FlashSaleItem::getSort));
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", sale.getId());
            map.put("name", sale.getName());
            map.put("startTime", sale.getStartTime());
            map.put("endTime", sale.getEndTime());
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (FlashSaleItem item : items) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("id", item.getId());
                im.put("skuId", item.getSkuId());
                im.put("spuId", item.getSpuId());
                im.put("flashPrice", item.getFlashPrice());
                im.put("stock", item.getStock());
                im.put("sold", item.getSold());
                im.put("limitPerUser", item.getLimitPerUser());
                itemList.add(im);
            }
            map.put("items", itemList);
            result.add(map);
        }
        return result;
    }

    @Override
    public Map<String, Object> getFlashSaleItemDetail(Long itemId) {
        FlashSaleItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BadRequestException("秒杀商品不存在");
        FlashSale sale = getById(item.getFlashSaleId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("skuId", item.getSkuId());
        map.put("spuId", item.getSpuId());
        map.put("flashPrice", item.getFlashPrice());
        map.put("stock", item.getStock());
        map.put("limitPerUser", item.getLimitPerUser());
        map.put("flashSaleName", sale != null ? sale.getName() : "");
        map.put("endTime", sale != null ? sale.getEndTime() : null);
        return map;
    }

    @Override
    @Transactional
    public void placeFlashOrder(Long itemId, Long userId) {
        String lockKey = "lock:flash:" + itemId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            // 扣库存
            int result = flashSaleMapper.deductStock(itemId);
            if (result <= 0) {
                throw new BadRequestException("秒杀库存不足");
            }
            log.info("秒杀下单成功: itemId={}, userId={}", itemId, userId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
