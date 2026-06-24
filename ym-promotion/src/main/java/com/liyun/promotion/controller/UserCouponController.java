package com.liyun.promotion.controller;

import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.vo.UserCouponVO;
import com.liyun.promotion.query.UserCouponQuery;
import com.liyun.promotion.service.IUserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-coupons")
@Tag(name = "用户优惠券相关接口")
@RequiredArgsConstructor
public class UserCouponController {

    private final IUserCouponService userCouponService;

    @PostMapping("/{couponId}/receive")
    @Operation(summary = "领取优惠券接口")
    public void receiveCoupon(@PathVariable("couponId") Long couponId) {
        userCouponService.receiveCoupon(couponId);
    }

    @PostMapping("/{code}/exchange")
    @Operation(summary = "兑换优惠券接口")
    public void exchangeCoupon(@PathVariable("code") String code) {
        userCouponService.exchangeCoupon(code);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户优惠券接口")
    public PageDTO<UserCouponVO> pageQueryUserCoupons(UserCouponQuery query) {
        return userCouponService.pageQueryUserCoupons(query);
    }
}
