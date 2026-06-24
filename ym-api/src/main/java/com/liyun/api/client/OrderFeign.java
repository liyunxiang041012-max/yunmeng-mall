package com.liyun.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单服务 Feign 客户端
 */
@FeignClient(name = "ym-pay", contextId = "orderFeign")
public interface OrderFeign {

    /** 查询用户订单列表 */
    @GetMapping("/order/list")
    Map<String, Object> getUserOrders();

    /** 查询订单详情 */
    @GetMapping("/order/detail/{orderId}")
    Map<String, Object> getOrderDetail(@PathVariable("orderId") String orderId);

    /** 取消订单 */
    @PutMapping("/order/cancel/{orderId}")
    Map<String, Object> cancelOrder(@PathVariable("orderId") String orderId);

    /** 查询店铺订单列表（分页，含商品、买家信息） */
    @GetMapping("/order/shop-list")
    Map<String, Object> getShopOrders(
            @RequestParam("shopId") Long shopId,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword);

    /** 查询店铺订单详情 */
    @GetMapping("/order/shop-detail/{orderId}")
    Map<String, Object> getShopOrderDetail(@PathVariable("orderId") String orderId);

    /** 店铺仪表盘统计 */
    @GetMapping("/order/shop-stats")
    Map<String, Object> getShopStats(@RequestParam("shopId") Long shopId);

    /** 收入趋势 */
    @GetMapping("/order/shop-revenue")
    Map<String, Object> getShopRevenue(
            @RequestParam("shopId") Long shopId,
            @RequestParam(value = "period", defaultValue = "7d") String period);

    /** 发货 */
    @PutMapping("/order/shop-ship/{orderId}")
    Map<String, Object> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("trackingNo") String trackingNo);
}
