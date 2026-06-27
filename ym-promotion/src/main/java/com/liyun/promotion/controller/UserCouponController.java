package com.liyun.promotion.controller;

import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.promotion.domain.po.Coupon;
import com.liyun.promotion.domain.po.UserCoupon;
import com.liyun.promotion.domain.vo.UserCouponVO;
import com.liyun.promotion.query.UserCouponQuery;
import com.liyun.promotion.service.IUserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/user-coupons")
@Tag(name = "用户优惠券相关接口")
@RequiredArgsConstructor
public class UserCouponController {

    private final IUserCouponService userCouponService;

    @PostMapping("/{couponId}/receive")
    @Operation(summary = "领取优惠券接口")
    public Result<Void> receiveCoupon(@PathVariable("couponId") Long couponId) {
        userCouponService.receiveCoupon(couponId);
        return Result.success();
    }

    @PostMapping("/{code}/exchange")
    @Operation(summary = "兑换优惠券接口")
    public Result<Void> exchangeCoupon(@PathVariable("code") String code) {
        userCouponService.exchangeCoupon(code);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户优惠券接口")
    public Result<PageDTO<UserCouponVO>> pageQueryUserCoupons(UserCouponQuery query) {
        return Result.success(userCouponService.pageQueryUserCoupons(query));
    }

    @PostMapping("/{id}/use")
    @Operation(summary = "使用优惠券（支付时调用，返回折扣信息）")
    public Result<Map<String, Object>> useCoupon(
            @PathVariable("id") Long id,
            @RequestParam("orderAmount") Long orderAmount) {
        UserCoupon uc = userCouponService.useCoupon(id, orderAmount);
        Coupon coupon = userCouponService.getCouponByUserCouponId(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCouponId", uc.getId());
        result.put("discountType", coupon != null ? coupon.getDiscountType().getValue() : 0);
        result.put("discountValue", coupon != null ? coupon.getDiscountValue() : 0);
        result.put("thresholdAmount", coupon != null ? coupon.getThresholdAmount() : 0);
        result.put("maxDiscountAmount", coupon != null ? coupon.getMaxDiscountAmount() : 0);
        return Result.success(result);
    }
}
