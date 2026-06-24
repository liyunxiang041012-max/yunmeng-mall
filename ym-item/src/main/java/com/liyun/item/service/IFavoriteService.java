package com.liyun.item.service;

import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.Favorite;
import com.liyun.item.domain.vo.FavoriteItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户收藏 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
public interface IFavoriteService extends IService<Favorite> {

    /**
     * 切换收藏状态
     * @param userId 用户ID
     * @param itemId 商品ID
     * @return true: 已收藏，false: 已取消收藏
     */
    boolean toggleFavorite(Long userId, Long itemId);

    /**
     * 检查是否已收藏
     * @param userId 用户ID
     * @param itemId 商品ID
     * @return true: 已收藏，false: 未收藏
     */
    boolean checkFavorite(Long userId, Long itemId);

    /**
     * 获取用户收藏列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页条数
     * @return 收藏列表分页
     */
    PageDTO<FavoriteItemVO> getMyFavorites(Long userId, Integer page, Integer size);
}
