package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.api.client.UserFeign;
import com.liyun.api.dto.RegisterShopDTO;
import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.DateUtils;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.pojo.Shop;
import com.liyun.item.domain.pojo.ShopRating;
import com.liyun.item.domain.vo.ShopCartVO;
import com.liyun.item.mapper.ShopMapper;
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
