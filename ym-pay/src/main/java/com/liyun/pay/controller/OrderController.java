package com.liyun.pay.controller;

import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.dto.UpdateOrderStatusDTO;
import com.liyun.pay.domain.pojo.Order;
import com.liyun.pay.domain.vo.CreateOrderVO;
import com.liyun.pay.service.IOrderService;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单相关接口")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<CreateOrderVO> create(@Valid @RequestBody OrderDTO dto) {
        CreateOrderVO vo = orderService.createOrder(dto);
        return Result.success(vo);
    }

    @Operation(summary = "查询订单列表")
    @GetMapping("/list")
    public Result<List<Order>> list() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        List<Order> list = orderService.orderList(userId);
        return Result.success(list);
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/detail/{orderId}")
    public Result<Order> detail(@PathVariable String orderId) {
        Order order = orderService.getOrderDetail(orderId);
        return Result.success(order);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/cancel/{orderId}")
    public Result<Void> cancel(@PathVariable String orderId) {
        orderService.cancelOrder(orderId);
        return Result.success();
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody UpdateOrderStatusDTO dto) {
        orderService.updateOrderStatus(dto.getOrderId(), dto.getStatus());
        return Result.success();
    }

    // ==================== 商家端 ====================

    @Operation(summary = "查询店铺订单列表（含商品、买家信息）")
    @GetMapping("/shop-list")
    public Result<PageDTO<Map<String, Object>>> shopOrderList(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(orderService.shopOrderList(shopId, page, size, status, keyword));
    }

    @Operation(summary = "查询店铺订单详情")
    @GetMapping("/shop-detail/{orderId}")
    public Result<Map<String, Object>> shopOrderDetail(@PathVariable String orderId) {
        return Result.success(orderService.getShopOrderDetail(orderId));
    }

    @Operation(summary = "店铺仪表盘统计")
    @GetMapping("/shop-stats")
    public Result<Map<String, Object>> shopStats(@RequestParam Long shopId) {
        return Result.success(orderService.getShopStats(shopId));
    }

    @Operation(summary = "收入趋势")
    @GetMapping("/shop-revenue")
    public Result<List<Map<String, Object>>> shopRevenue(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "7d") String period) {
        return Result.success(orderService.getRevenueTrend(shopId, period));
    }

    @Operation(summary = "发货")
    @PutMapping("/shop-ship/{orderId}")
    public Result<Void> shipOrder(
            @PathVariable String orderId,
            @RequestParam String trackingNo) {
        orderService.shipOrder(orderId, trackingNo);
        return Result.success();
    }

    // ==================== 管理员端 ====================

    @Operation(summary = "管理员 - 平台订单统计（总订单数、总GMV）")
    @GetMapping("/admin-stats")
    public Result<Map<String, Object>> adminStats() {
        return Result.success(orderService.getAdminStats());
    }

    @Operation(summary = "管理员 - 最近订单列表")
    @GetMapping("/admin-recent")
    public List<Map<String, Object>> adminRecentOrders() {
        log.info("[ADMIN-CTRL] admin-recent called");
        return orderService.getAdminRecentOrders();
    }

    @Operation(summary = "管理员 - 热销商品排行")
    @GetMapping("/admin-top-products")
    public List<Map<String, Object>> adminTopProducts() {
        log.info("[ADMIN-CTRL] admin-top-products called");
        return orderService.getAdminTopProducts();
    }

    @Operation(summary = "管理员 - 平台收入趋势")
    @GetMapping("/admin-revenue")
    public Result<Map<String, Object>> adminRevenue(@RequestParam(defaultValue = "7") int period) {
        return Result.success(orderService.getAdminRevenue(period));
    }
}
