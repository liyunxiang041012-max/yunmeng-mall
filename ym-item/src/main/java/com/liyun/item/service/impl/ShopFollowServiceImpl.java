package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.Shop;
import com.liyun.item.domain.pojo.ShopFollow;
import com.liyun.item.domain.vo.FollowShopVO;
import com.liyun.item.mapper.ShopFollowMapper;
import com.liyun.item.mapper.ShopMapper;
import com.liyun.item.service.IShopFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户关注店铺 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
@Service
@RequiredArgsConstructor
public class ShopFollowServiceImpl extends ServiceImpl<ShopFollowMapper, ShopFollow> implements IShopFollowService {

    private final ShopMapper shopMapper;

    @Override
    public boolean toggleFollow(Long userId, Long shopId) {
        // 校验用户ID
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 先查询是否已关注
        ShopFollow shopFollow = getOne(new LambdaQueryWrapper<ShopFollow>()
                .eq(ShopFollow::getUserId, userId)
                .eq(ShopFollow::getShopId, shopId)
        );

        if (shopFollow != null) {
            // 已关注，取消关注
            removeById(shopFollow.getId());
            return false;
        } else {
            // 未关注，添加关注
            shopFollow = new ShopFollow();
            shopFollow.setUserId(userId);
            shopFollow.setShopId(shopId);
            save(shopFollow);
            return true;
        }
    }

    @Override
    public boolean checkFollow(Long userId, Long shopId) {
        // 校验用户ID
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        return count(new LambdaQueryWrapper<ShopFollow>()
                .eq(ShopFollow::getUserId, userId)
                .eq(ShopFollow::getShopId, shopId)
        ) > 0;
    }

    @Override
    public Long countFans(Long shopId) {
        return count(new LambdaQueryWrapper<ShopFollow>()
                .eq(ShopFollow::getShopId, shopId)
        );
    }

    public List<FollowShopVO> getMyFollows(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 1. 查询用户所有关注记录
        List<ShopFollow> follows = list(new LambdaQueryWrapper<ShopFollow>()
                .eq(ShopFollow::getUserId, userId)
                .orderByDesc(ShopFollow::getCreateTime));

        if (follows.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集店铺ID
        List<Long> shopIds = follows.stream()
                .map(ShopFollow::getShopId)
                .collect(Collectors.toList());

        // 3. 批量查询店铺信息
        List<Shop> shops = shopMapper.selectBatchIds(shopIds);
        Map<Long, Shop> shopMap = shops.stream()
                .collect(Collectors.toMap(Shop::getId, s -> s));

        // 4. 组装返回
        return follows.stream().map(f -> {
            FollowShopVO vo = new FollowShopVO();
            vo.setId(f.getId());
            vo.setShopId(f.getShopId());
            vo.setCreateTime(f.getCreateTime());

            Shop shop = shopMap.get(f.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getShopName());
                vo.setLogo(shop.getLogo());
                vo.setDescription(shop.getDescription());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageDTO<FollowShopVO> pageMyFollows(Long userId, Integer page, Integer size) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        Page<ShopFollow> p = new Page<>(page, size);
        Page<ShopFollow> result = page(p, new LambdaQueryWrapper<ShopFollow>()
                .eq(ShopFollow::getUserId, userId)
                .orderByDesc(ShopFollow::getCreateTime));
        List<FollowShopVO> voList = toVoList(result.getRecords());
        return PageDTO.of(result, voList);
    }

    private List<FollowShopVO> toVoList(List<ShopFollow> follows) {
        if (follows.isEmpty()) return Collections.emptyList();
        List<Long> shopIds = follows.stream().map(ShopFollow::getShopId).collect(Collectors.toList());
        Map<Long, Shop> shopMap = shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, s -> s));
        return follows.stream().map(f -> {
            FollowShopVO vo = new FollowShopVO();
            vo.setId(f.getId());
            vo.setShopId(f.getShopId());
            vo.setCreateTime(f.getCreateTime());
            Shop shop = shopMap.get(f.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getShopName());
                vo.setLogo(shop.getLogo());
                vo.setDescription(shop.getDescription());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
