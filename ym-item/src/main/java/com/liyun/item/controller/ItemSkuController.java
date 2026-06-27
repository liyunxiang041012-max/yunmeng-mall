package com.liyun.item.controller;

import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.service.IItemSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/sku", "/skus"})
@RequiredArgsConstructor
@Tag(name = "SKU管理", description = "SKU查询接口")
public class ItemSkuController {

    private final IItemSkuService itemSkuService;

    @Operation(summary = "获取SKU信息")
    @GetMapping("/info/{skuId}")
    public Result<SkuInfoDTO> getSkuInfo(@PathVariable("skuId") Long skuId) {
        SkuInfoDTO dto = itemSkuService.getSkuInfo(skuId);
        if (dto == null) {
            return Result.fail("商品不存在");
        }
        return Result.success(dto);
    }

    @Operation(summary = "批量获取SKU信息")
    @PostMapping("/batch-info")
    public Result<List<SkuInfoDTO>> batchGetSkuInfo(@RequestBody List<Long> skuIds) {
        return Result.success(itemSkuService.batchGetSkuInfo(skuIds));
    }

    @Operation(summary = "批量获取SKU价格（前端兼容接口）")
    @PostMapping("/prices/batch")
    public Result<List<SkuInfoDTO>> getSkuPrices(@RequestBody List<Long> skuIds) {
        return Result.success(itemSkuService.batchGetSkuInfo(skuIds));
    }

    /** 内部调用：批量扣减库存 */
    @Operation(summary = "批量扣减库存(内部)")
    @PostMapping("/deduct-stock")
    public Map<String, Object> deductStock(@RequestBody Map<Long, Integer> skuQtyMap) {
        log.info("[SKU-CTRL] deductStock: {}", skuQtyMap);
        try {
            itemSkuService.batchDeductStock(skuQtyMap);
            return Map.of("success", true);
        } catch (Exception e) {
            log.error("[SKU-CTRL] deductStock failed: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /** 内部调用：批量增加销量 */
    @Operation(summary = "批量增加销量(内部)")
    @PostMapping("/add-sold")
    public Map<String, Object> addSold(@RequestBody Map<Long, Integer> itemQtyMap) {
        log.info("[SKU-CTRL] addSold: {}", itemQtyMap);
        try {
            itemSkuService.batchAddSold(itemQtyMap);
            return Map.of("success", true);
        } catch (Exception e) {
            log.error("[SKU-CTRL] addSold failed: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
