package com.liyun.pay.controller;

import com.liyun.pay.domain.pojo.OrderItem;
import com.liyun.pay.service.IOrderItemService;
import com.liyun.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order-item")
@Tag(name = "订单明细管理", description = "订单明细相关接口")
@RequiredArgsConstructor
public class OrderItemController {

    private final IOrderItemService orderItemService;

    @Operation(summary = "查询订单明细列表")
    @GetMapping("/list/{orderId}")
    public Result<List<OrderItem>> list(@PathVariable String orderId) {
        List<OrderItem> list = orderItemService.getOrderItems(orderId);
        return Result.success(list);
    }
}
