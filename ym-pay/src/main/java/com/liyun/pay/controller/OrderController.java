package com.liyun.pay.controller;

import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.dto.UpdateOrderStatusDTO;
import com.liyun.pay.domain.pojo.Order;
import com.liyun.pay.service.IOrderService;
import com.liyun.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单相关接口")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<String> create(@Valid @RequestBody OrderDTO dto) {
        // TODO: 从上下文获取userId
       String orderId =  orderService.createOrder(dto);
        return Result.success(orderId);
    }

    @Operation(summary = "查询订单列表")
    @GetMapping("/list")
    public Result<List<Order>> list() {
        // TODO: 从上下文获取userId
        Long userId = 1L;
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
}
