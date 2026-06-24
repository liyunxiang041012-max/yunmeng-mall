package com.liyun.promotion.handler;

import com.liyun.promotion.service.ICouponService;
import com.liyun.promotion.service.IUserCouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponJobHandler {

    private final ICouponService couponService;
    private final IUserCouponService userCouponService;

    @XxlJob("couponBeginIssue")
    public void onTimeBeginIssue() {
        couponService.onTimeBeginIssue();
    }

    @XxlJob("couponEndIssue")
    public void onTimeEndIssue() {
        couponService.onTimeEndIssue();
    }

    /** 扫描过期用户券 */
    @XxlJob("couponExpireCheck")
    public void handleExpiredCoupons() {
        userCouponService.handleExpiredCoupons();
    }
}
