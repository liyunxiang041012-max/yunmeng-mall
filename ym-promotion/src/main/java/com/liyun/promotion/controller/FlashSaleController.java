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

/**
 * 秒杀活动 - 用户端
 * 支持查询"进行中"和"预热中"的秒杀商品
 * 前端根据 status + countdownSeconds 展示倒计时和标签
 */
@RestController
@RequestMapping("/flash-sales")
@RequiredArgsConstructor
@Tag(name = "秒杀活动（用户端）", description = "查询秒杀商品、秒杀下单")
public class FlashSaleController {

    private final IFlashSaleService flashSaleService;

    @GetMapping("/ongoing")
    @Operation(summary = "查询进行中的秒杀商品（已到时间、正在秒杀）")
    public Result<List<Map<String, Object>>> getOngoing() {
        return Result.success(flashSaleService.getOngoingFlashSales());
    }

    @GetMapping("/warming")
    @Operation(summary = "查询预热中的秒杀商品（3天内即将开始）")
    public Result<List<Map<String, Object>>> getWarming() {
        return Result.success(flashSaleService.getWarmingFlashSales());
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "查询秒杀商品详情（含倒计时秒数）")
    public Result<Map<String, Object>> getItemDetail(@PathVariable Long itemId) {
        return Result.success(flashSaleService.getFlashSaleItemDetail(itemId));
    }

    @PostMapping("/items/{itemId}/order")
    @Operation(summary = "秒杀下单（仅进行中可下单）")
    public Result<Void> placeOrder(@PathVariable Long itemId) {
        Long userId = UserContext.getUserId();
        flashSaleService.placeFlashOrder(itemId, userId);
        return Result.success();
    }
}
