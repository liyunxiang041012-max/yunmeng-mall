package com.liyun.item.service;

import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.ShopFollow;
import com.liyun.item.domain.vo.FollowShopVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户关注店铺 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
public interface IShopFollowService extends IService<ShopFollow> {

    /**
     * 切换关注状态
     * @param userId 用户ID
     * @param shopId 店铺ID
     * @return true: 已关注，false: 已取消关注
     */
    boolean toggleFollow(Long userId, Long shopId);

    /**
     * 检查是否已关注
     * @param userId 用户ID
     * @param shopId 店铺ID
     * @return true: 已关注，false: 未关注
     */
    boolean checkFollow(Long userId, Long shopId);

    /**
     * 统计店铺粉丝数
     * @param shopId 店铺ID
     * @return 粉丝数
     */
    Long countFans(Long shopId);

    /**
     * 获取用户关注的店铺列表（分页）
     */
    PageDTO<FollowShopVO> pageMyFollows(Long userId, Integer page, Integer size);
}
