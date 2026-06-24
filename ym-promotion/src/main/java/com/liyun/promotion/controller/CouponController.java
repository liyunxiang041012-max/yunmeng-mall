package com.liyun.promotion.controller;

import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.dto.CouponFormDTO;
import com.liyun.promotion.domain.dto.CouponIssueFormDTO;
import com.liyun.promotion.domain.vo.CouponDetailVO;
import com.liyun.promotion.domain.vo.CouponPageVO;
import com.liyun.promotion.domain.vo.CouponVO;
import com.liyun.promotion.query.CouponQuery;
import com.liyun.promotion.service.ICouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "优惠券相关接口")
public class CouponController {

    private final ICouponService couponService;

    @PostMapping
    @Operation(summary = "新增优惠券接口")
    public void saveCoupon(@RequestBody @Valid CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询优惠券接口")
    public PageDTO<CouponPageVO> pageQueryCoupon(CouponQuery query) {
        return couponService.pageQueryCoupon(query);
    }

    @PutMapping("/{id}/issue")
    @Operation(summary = "开始发放优惠券接口")
    public void beginIssue(@RequestBody @Valid CouponIssueFormDTO dto) {
        couponService.beginIssue(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新优惠券接口")
    public void updateCoupon(@PathVariable Long id, @RequestBody @Valid CouponFormDTO dto) {
        couponService.updateCoupon(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除优惠券接口")
    public void deleteCoupon(@PathVariable Long id) {
        couponService.removeByIdAndCouponScope(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询优惠券接口")
    public CouponDetailVO getCouponById(@PathVariable Long id) {
        return couponService.getCouponById(id);
    }

    @PutMapping("/{id}/pause")
    @Operation(summary = "暂停发放优惠券接口")
    public void pauseIssue(@PathVariable Long id) {
        couponService.pauseIssue(id);
    }

    @GetMapping("/list")
    @Operation(summary = "查询发放中的优惠券列表")
    public List<CouponVO> queryIssuingCoupon() {
        return couponService.queryIssuingCoupon();
    }
}
