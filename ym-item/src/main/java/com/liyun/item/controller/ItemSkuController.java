package com.liyun.item.controller;

import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.service.IItemSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sku")
@RequiredArgsConstructor
@Tag(name = "SKU管理", description = "SKU查询接口")
public class ItemSkuController {

    private final IItemSkuService itemSkuService;

    @Operation(summary = "获取SKU信息")
    @GetMapping("/info/{skuId}")
    public SkuInfoDTO getSkuInfo(@PathVariable("skuId") Long skuId) {
        SkuInfoDTO dto = itemSkuService.getSkuInfo(skuId);
        if (dto == null) {
            throw new RuntimeException("商品不存在");
        }
        return dto;
    }

    @Operation(summary = "批量获取SKU信息")
    @PostMapping("/batch-info")
    public List<SkuInfoDTO> batchGetSkuInfo(@RequestBody List<Long> skuIds) {
        return itemSkuService.batchGetSkuInfo(skuIds);
    }
}
