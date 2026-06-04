package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.api.client.UserFeign;
import com.liyun.api.dto.RegisterShopDTO;
import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.DateUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.pojo.Shop;
import com.liyun.item.domain.pojo.ShopRating;
import com.liyun.item.domain.vo.ItemVO;
import com.liyun.item.domain.vo.ShopCartVO;
import com.liyun.item.domain.vo.ShopDetailVO;
import com.liyun.item.mapper.ShopMapper;
import com.liyun.item.service.IItemService;
import com.liyun.item.service.IShopFollowService;
import com.liyun.item.service.IShopRatingService;
import com.liyun.item.service.IShopService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final UserFeign userFeign;
    private final IShopRatingService shopRatingService;
    private final IShopFollowService shopFollowService;
    private final IItemService itemService;

    @Override
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

        saveShopAndRating(userId, dto);
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
    public ShopDetailVO getShopDetail(Long shopId) {
        // 1. 查询店铺基本信息
        Shop shop = getById(shopId);
        if (shop == null) {
            throw new RuntimeException("店铺不存在");
        }

        ShopDetailVO vo = new ShopDetailVO();
        vo.setId(shop.getId());
        vo.setShopName(shop.getShopName());
        vo.setLogo(shop.getLogo());
        vo.setDescription(shop.getDescription());

        // 2. 查询店铺评分信息
        ShopRating rating = shopRatingService.getOne(new LambdaQueryWrapper<ShopRating>()
                .eq(ShopRating::getShopId, shopId)
        );
        if (rating != null) {
            vo.setScore(String.valueOf(rating.getAverageScore()));
            vo.setGoodRate(rating.getGoodRate() + "%");
            // 平均发货时间，暂时写死为 "24h"，后续可以根据订单表计算
            vo.setAvgShipTime("24h");
        } else {
            vo.setScore("0.0");
            vo.setGoodRate("0%");
            vo.setAvgShipTime("24h");
        }

        // 3. 统计总销量（所有商品的 sold 字段求和）
        List<Item> items = itemService.list(new LambdaQueryWrapper<Item>()
                .eq(Item::getShopId, shopId)
                .eq(Item::getStatus, 1)
                .eq(Item::getDeleted, 0)
        );
        long totalSales = items.stream().mapToLong(item -> item.getSold() != null ? item.getSold() : 0).sum();
        vo.setTotalSales(totalSales);

        // 4. 统计在售商品总数
        vo.setItemCount(items.size());

        // 5. 统计粉丝数
        Long fansCount = shopFollowService.countFans(shopId);
        vo.setFansCount(fansCount);

        return vo;
    }

    @Override
    public PageDTO<ItemVO> getShopItems(Long shopId, String sort, Integer page, Integer size) {
        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        if (sort == null || sort.isEmpty()) {
            sort = "default";
        }

        // 查询店铺信息
        Shop shop = getById(shopId);
        if (shop == null) {
            return PageDTO.empty(0L, 0L);
        }

        // 构建查询条件
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Item::getShopId, shopId)
               .eq(Item::getStatus, 1)
               .eq(Item::getDeleted, 0);

        // 根据排序方式设置排序规则
        switch (sort) {
            case "sales":
                wrapper.orderByDesc(Item::getSold);
                break;
            case "price":
                wrapper.orderByAsc(Item::getPrice);
                break;
            case "newest":
                wrapper.orderByDesc(Item::getCreateTime);
                break;
            case "default":
            default:
                // 综合排序，默认按创建时间倒序
                wrapper.orderByDesc(Item::getCreateTime);
                break;
        }

        // 执行分页查询
        Page<Item> pageParam = new Page<>(page, size);
        Page<Item> resultPage = itemService.page(pageParam, wrapper);

        // 转换为 ItemVO
        String shopName = shop.getShopName();
        List<ItemVO> voList = resultPage.getRecords().stream().map(item -> {
            ItemVO vo = new ItemVO();
            vo.setId(item.getId());
            vo.setName(item.getName());
            vo.setImage(item.getImage());
            vo.setPrice(item.getPrice());
            vo.setSold(item.getSold());
            vo.setShopName(shopName);
            vo.setStatus(item.getStatus());
            return vo;
        }).collect(Collectors.toList());

        // 构建分页结果
        PageDTO<ItemVO> pageDTO = PageDTO.of(resultPage, voList);

        return pageDTO;
    }

    @Transactional
    protected void saveShopAndRating(Long userId, ShopDTO dto) {
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
}
