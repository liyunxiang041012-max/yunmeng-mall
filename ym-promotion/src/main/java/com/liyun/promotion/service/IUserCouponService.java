package com.liyun.promotion.service;

import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.po.Coupon;
import com.liyun.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.promotion.domain.vo.UserCouponVO;
import com.liyun.promotion.query.UserCouponQuery;

public interface IUserCouponService extends IService<UserCoupon> {

    void receiveCoupon(Long couponId);

    void checkAndCreateUserCoupon(Long userId, Coupon coupon);

    void exchangeCoupon(String code);

    PageDTO<UserCouponVO> pageQueryUserCoupons(UserCouponQuery query);

    /** 扫描并标记过期用户券 */
    void handleExpiredCoupons();

    /** 使用优惠券（支付时调用） */
    UserCoupon useCoupon(Long userCouponId, Long orderAmount);

    /** 根据用户券ID查询优惠券规则 */
    Coupon getCouponByUserCouponId(Long userCouponId);
}
