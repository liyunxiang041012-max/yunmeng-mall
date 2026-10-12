package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.exception.BizException;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.Favorite;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.vo.FavoriteItemVO;
import com.liyun.item.mapper.FavoriteMapper;
import com.liyun.item.service.IFavoriteService;
import com.liyun.item.service.IItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户收藏 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {

    private final IItemService itemService;

    @Override
    public boolean toggleFavorite(Long userId, Long itemId) {
        // 校验用户ID
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 先查询是否已收藏
        Favorite favorite = getOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId)
        );

        if (favorite != null) {
            // 已收藏，取消收藏
            removeById(favorite.getId());
            return false;
        } else {
            // 未收藏，添加收藏
            favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setItemId(itemId);
            save(favorite);
            return true;
        }
    }

    @Override
    public boolean checkFavorite(Long userId, Long itemId) {
        // 校验用户ID
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        return count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId)
        ) > 0;
    }

    @Override
    public PageDTO<FavoriteItemVO> getMyFavorites(Long userId, Integer page, Integer size) {
        // 校验用户ID
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }

        // 1. 分页查询收藏记录
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .orderByDesc(Favorite::getCreateTime);

        Page<Favorite> pageParam = new Page<>(page, size);
        Page<Favorite> favoritePage = page(pageParam, wrapper);

        if (favoritePage.getRecords().isEmpty()) {
            return PageDTO.empty(favoritePage);
        }

        // 2. 获取收藏的商品ID列表
        List<Long> itemIds = favoritePage.getRecords().stream()
                .map(Favorite::getItemId)
                .collect(Collectors.toList());

        // 3. 批量查询商品信息
        List<Item> items = itemService.listByIds(itemIds);

        // 4. 组装返回数据
        List<FavoriteItemVO> voList = favoritePage.getRecords().stream().map(fav -> {
            FavoriteItemVO vo = new FavoriteItemVO();
            vo.setId(fav.getId());
            vo.setItemId(fav.getItemId());
            vo.setCreateTime(fav.getCreateTime());

            // 查找对应的商品信息
            Item item = items.stream()
                    .filter(i -> i.getId().equals(fav.getItemId()))
                    .findFirst()
                    .orElse(null);

            if (item != null) {
                vo.setName(item.getName());
                vo.setItemName(item.getName());
                vo.setMainImage(item.getImage());
                vo.setImage(item.getImage());
                vo.setPrice(item.getPrice());
                vo.setSalesCount(item.getSold());
            }

            return vo;
        }).collect(Collectors.toList());

        return PageDTO.of(favoritePage, voList);
    }
}
