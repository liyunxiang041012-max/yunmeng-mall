package com.liyun.api.client;

import com.liyun.api.dto.CouponInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 促销服务 Feign 客户端
 */
@FeignClient(name = "ym-promotion", contextId = "promotionFeign")
public interface PromotionFeign {

    /** 分页查询用户优惠券 */
    @GetMapping("/user-coupons/page")
    Map<String, Object> pageQueryUserCoupons(@RequestParam("status") Integer status);

    /** 查询发放中的优惠券列表 */
    @GetMapping("/coupons/list")
    List<CouponInfoDTO> queryIssuingCoupons();
}
