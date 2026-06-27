package com.liyun.promotion.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.Result;
import com.liyun.promotion.service.IFlashSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flash-sales")
@RequiredArgsConstructor
@Tag(name = "秒杀活动（用户端）")
public class FlashSaleController {

    private final IFlashSaleService flashSaleService;

    @GetMapping
    @Operation(summary = "查询当前进行中的秒杀活动列表")
    public Result<List<Map<String, Object>>> getCurrent() {
        return Result.success(flashSaleService.getCurrentFlashSales());
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "查询秒杀商品详情")
    public Result<Map<String, Object>> getItemDetail(@PathVariable Long itemId) {
        return Result.success(flashSaleService.getFlashSaleItemDetail(itemId));
    }

    @PostMapping("/items/{itemId}/order")
    @Operation(summary = "秒杀下单")
    public Result<Void> placeOrder(@PathVariable Long itemId) {
        Long userId = UserContext.getUserId();
        flashSaleService.placeFlashOrder(itemId, userId);
        return Result.success();
    }
}
