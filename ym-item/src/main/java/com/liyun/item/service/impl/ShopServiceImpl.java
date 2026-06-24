package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.api.client.OrderFeign;
import com.liyun.api.client.UserFeign;
import com.liyun.api.dto.RegisterShopDTO;
import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.DateUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.dto.ShopEntryDTO;
import com.liyun.item.domain.dto.ShopSetupDTO;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.pojo.Shop;
import com.liyun.item.domain.pojo.ShopRating;
import com.liyun.item.domain.vo.ShopCartVO;
import com.liyun.item.domain.vo.ShopEntryVO;
import com.liyun.item.mapper.ShopMapper;
import com.liyun.item.service.IShopRatingService;
import com.liyun.item.service.IShopService;
import com.liyun.item.service.IItemService;
import com.liyun.item.service.impl.OssUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final UserFeign userFeign;
    private final IShopRatingService shopRatingService;
    private final OssUploadService ossUploadService;
    private final IItemService itemService;
    private final OrderFeign orderFeign;

    @Override
    @Transactional
    public void addShop(ShopDTO dto, String ip) {
        RegisterShopDTO registerShopDTO = new RegisterShopDTO();
        BeanUtils.copyProperties(dto, registerShopDTO);

        Long userId;
        try {
            userId = userFeign.registerShop(registerShopDTO);
        } catch (Exception e) {
            log.error("调用用户服务注册商家失败", e);
            throw new RuntimeException("用户服务不可用，请稍后再试", e);
        }

        if (userId == null) {
            log.warn("用户服务注册失败");
            throw new RuntimeException("注册商家失败");
        }

        // 注册成功，不在这里创建 Shop。商家登录后通过 /it/shop/setup 设置店铺信息
        log.info("商家账号注册成功，userId: {}, 等待首次登录设置店铺信息", userId);
    }

    @Override
    @Transactional
    public void setupShop(Long userId, ShopSetupDTO dto) {
        // 检查是否已有店铺
        Shop existingShop = lambdaQuery().eq(Shop::getUserId, userId).one();
        if (existingShop != null) {
            // 更新已有店铺信息
            existingShop.setShopName(dto.getShopName());
            existingShop.setLogo(dto.getLogo());
            existingShop.setDescription(dto.getDescription());
            existingShop.setUpdateTime(DateUtils.now());
            updateById(existingShop);
            log.info("店铺信息已更新，shopId: {}", existingShop.getId());
            return;
        }

        // 新建店铺
        saveShopAndRating(userId, dto);
        log.info("店铺创建成功，userId: {}", userId);
    }

    @Override
    public Map<String, Object> getShopStatus() {
        Long userId = UserContext.getUserId();
        Shop shop = lambdaQuery().eq(Shop::getUserId, userId).one();

        if (shop == null) {
            return Map.of("hasShop", false);
        }
        return Map.of("hasShop", true, "shopId", shop.getId(), "shopName", shop.getShopName());
    }

    @Override
    public ShopCartVO getCartShopInfo(Long id) {
        Shop shop = getById(id);
        if (shop == null){
            return null;
        }
        ShopCartVO vo = new ShopCartVO();
        vo.setId(shop.getId());
        vo.setName(shop.getShopName());
        return vo;
    }

    @Override
    public List<ShopCartVO> batchGetCartShopInfo(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Shop> shops = this.list(new LambdaQueryWrapper<Shop>()
                .in(Shop::getId, shopIds));

        if (shops.isEmpty()) {
            return Collections.emptyList();
        }

        return shops.stream().map(shop -> {
            ShopCartVO vo = new ShopCartVO();
            vo.setId(shop.getId());
            vo.setName(shop.getShopName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ShopInfoDTO getShopInfo(Long shopId) {
        Shop shop = getById(shopId);
        if (shop == null) {
            return null;
        }

        ShopInfoDTO dto = new ShopInfoDTO();
        dto.setId(shop.getId());
        dto.setUserId(shop.getUserId());
        dto.setShopName(shop.getShopName());
        dto.setLogo(shop.getLogo());
        dto.setDescription(shop.getDescription());
        dto.setStatus(shop.getStatus());

        return dto;
    }

    @Override
    public List<ShopInfoDTO> batchGetShopInfo(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Shop> shops = this.list(new LambdaQueryWrapper<Shop>()
                .in(Shop::getId, shopIds));

        if (shops.isEmpty()) {
            return Collections.emptyList();
        }

        return shops.stream().map(shop -> {
            ShopInfoDTO dto = new ShopInfoDTO();
            dto.setId(shop.getId());
            dto.setUserId(shop.getUserId());
            dto.setShopName(shop.getShopName());
            dto.setLogo(shop.getLogo());
            dto.setDescription(shop.getDescription());
            dto.setStatus(shop.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public String uploadShopAvatar(MultipartFile file) {
        return ossUploadService.uploadShopAvatar(file);
    }

    @Override
    @Transactional
    public ShopEntryVO entryShop(Long userId, ShopEntryDTO dto) {
        // 检查是否已有店铺
        Shop existingShop = lambdaQuery().eq(Shop::getUserId, userId).one();
        if (existingShop != null) {
            // 更新已有店铺信息
            existingShop.setShopName(dto.getShopName());
            existingShop.setLogo(dto.getLogo());
            existingShop.setDescription(dto.getDescription());
            existingShop.setUpdateTime(DateUtils.now());
            updateById(existingShop);
            log.info("店铺信息已更新，shopId: {}", existingShop.getId());
            return buildEntryVO(existingShop, false);
        }

        // 新建店铺
        Shop shop = saveShopAndRatingForEntry(userId, dto);
        log.info("商家入驻成功，userId: {}", userId);
        return buildEntryVO(shop, true);
    }

    private ShopEntryVO buildEntryVO(Shop shop, boolean isNew) {
        ShopEntryVO vo = new ShopEntryVO();
        vo.setShopId(shop.getId());
        vo.setUserId(shop.getUserId());
        vo.setShopName(shop.getShopName());
        vo.setLogo(shop.getLogo());
        vo.setDescription(shop.getDescription());
        vo.setStatus(shop.getStatus());
        vo.setCreateTime(shop.getCreateTime());
        vo.setIsNew(isNew);
        return vo;
    }

    @Transactional
    protected void saveShopAndRating(Long userId, ShopSetupDTO dto) {
        Shop shop = new Shop();
        shop.setUserId(userId);
        shop.setShopName(dto.getShopName());
        shop.setLogo(dto.getLogo());
        shop.setDescription(dto.getDescription());
        shop.setStatus(1);
        shop.setDeleted(0);
        shop.setCreateTime(DateUtils.now());
        shop.setUpdateTime(DateUtils.now());

        save(shop);

        Long shopId = shop.getId();
        if (shopId == null) {
            shopId = lambdaQuery().eq(Shop::getUserId, userId).one().getId();
        }

        ShopRating shopRating = new ShopRating();
        shopRating.setShopId(shopId);
        shopRating.setLastUpdated(DateUtils.now());
        shopRatingService.save(shopRating);
    }

    @Transactional
    protected Shop saveShopAndRatingForEntry(Long userId, ShopEntryDTO dto) {
        Shop shop = new Shop();
        shop.setUserId(userId);
        shop.setShopName(dto.getShopName());
        shop.setLogo(dto.getLogo());
        shop.setDescription(dto.getDescription());
        shop.setStatus(1); // 营业状态
        shop.setDeleted(0);
        shop.setCreateTime(DateUtils.now());
        shop.setUpdateTime(DateUtils.now());

        save(shop);

        Long shopId = shop.getId();
        if (shopId == null) {
            shopId = lambdaQuery().eq(Shop::getUserId, userId).one().getId();
            shop.setId(shopId);
        }

        ShopRating shopRating = new ShopRating();
        shopRating.setShopId(shopId);
        shopRating.setLastUpdated(DateUtils.now());
        shopRatingService.save(shopRating);

        return shop;
    }

    // ==================== 商家仪表盘 ====================

    @Override
    public Shop getShopById(Long shopId) {
        Shop shop = getById(shopId);
        if (shop == null || shop.getDeleted() == 1) {
            throw new RuntimeException("店铺不存在");
        }
        return shop;
    }

    @Override
    public Map<String, Object> getShopStats() {
        Long userId = UserContext.getUserId();
        Shop shop = lambdaQuery().eq(Shop::getUserId, userId).one();
        if (shop == null) {
            throw new RuntimeException("您还未开设店铺");
        }

        Long shopId = shop.getId();
        long totalProducts = itemService.count(
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getShopId, shopId)
                        .eq(Item::getDeleted, 0));
        long activeProducts = itemService.count(
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getShopId, shopId)
                        .eq(Item::getStatus, 1)
                        .eq(Item::getDeleted, 0));

        // 构造基础数据
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("shopId", shopId);
        stats.put("shopName", shop.getShopName());
        stats.put("logo", shop.getLogo() != null ? shop.getLogo() : "");
        stats.put("status", shop.getStatus());
        stats.put("totalProducts", totalProducts);
        stats.put("activeProducts", activeProducts);

        // 从订单服务获取统计
        try {
            Map<String, Object> orderStats = orderFeign.getShopStats(shopId);
            if (orderStats != null) {
                stats.put("todaySales", orderStats.getOrDefault("todaySales", 0L));
                stats.put("todayOrders", orderStats.getOrDefault("todayOrders", 0));
                stats.put("totalRevenue", orderStats.getOrDefault("totalRevenue", 0L));
                stats.put("totalOrders", orderStats.getOrDefault("totalOrders", 0));
                stats.put("views", orderStats.getOrDefault("views", 0));
            }
        } catch (Exception e) {
            log.warn("获取订单统计数据失败", e);
            stats.put("todaySales", 0L);
            stats.put("todayOrders", 0);
            stats.put("totalRevenue", 0L);
            stats.put("totalOrders", 0);
            stats.put("views", 0);
        }

        return stats;
    }

    // ==================== 管理后台 CRUD ====================

    @Override
    public PageDTO<Shop> listShops(Integer page, Integer size, String keyword, Integer status) {
        Page<Shop> p = Page.of(page, size);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Shop::getShopName, keyword);
        }
        if (status != null) {
            wrapper.eq(Shop::getStatus, status);
        }
        wrapper.orderByDesc(Shop::getCreateTime);
        page(p, wrapper);
        return PageDTO.of(p, p.getRecords());
    }

    @Override
    public void updateShopInfo(Long shopId, String shopName, String logo, String description) {
        Shop shop = getById(shopId);
        if (shop == null) throw new RuntimeException("店铺不存在");
        if (shopName != null && !shopName.isBlank()) shop.setShopName(shopName);
        if (logo != null) shop.setLogo(logo);
        if (description != null) shop.setDescription(description);
        shop.setUpdateTime(DateUtils.now());
        updateById(shop);
    }

    @Override
    public void deleteShop(Long shopId) {
        Shop shop = getById(shopId);
        if (shop == null) throw new RuntimeException("店铺不存在");
        shop.setDeleted(1);
        shop.setUpdateTime(DateUtils.now());
        updateById(shop);
    }

    @Override
    public void toggleShopStatus(Long shopId) {
        Shop shop = getById(shopId);
        if (shop == null) throw new RuntimeException("店铺不存在");
        shop.setStatus(shop.getStatus() == 1 ? 0 : 1);
        shop.setUpdateTime(DateUtils.now());
        updateById(shop);
    }
}
