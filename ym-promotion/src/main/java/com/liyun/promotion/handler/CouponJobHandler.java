package com.liyun.promotion.handler;

import com.liyun.promotion.service.ICouponService;
import com.liyun.promotion.service.IUserCouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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

    /** 定时删除已结束优惠券（清理历史数据） */
    @XxlJob("couponDeleteFinished")
    public void deleteFinishedCoupons() {
        log.info("========================================");
        log.info("【优惠券清除】XXL-JOB 触发，开始扫描已结束优惠券...");
        long start = System.currentTimeMillis();
        try {
            couponService.deleteFinishedCoupons();
            log.info("【优惠券清除】扫描完成，耗时: {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("【优惠券清除】扫描失败", e);
        }
        log.info("========================================");
    }
}
