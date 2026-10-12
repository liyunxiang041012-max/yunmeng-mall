package com.liyun.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * 购物车服务 Feign 客户端
 * 注意：返回 Map<String, Object> 而非强类型 DTO，因为 ym-pay 的 Controller 用 Result<T> 包裹响应
 */
@FeignClient(name = "ym-pay", contextId = "cartFeign")
public interface CartFeign {

    /** 查询用户购物车列表 */
    @GetMapping("/cart/list")
    Map<String, Object> getUserCart();
}
