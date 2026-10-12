package com.liyun.promotion.controller;

import com.liyun.common.utils.Result;
import com.liyun.promotion.domain.po.FlashSale;
import com.liyun.promotion.domain.po.FlashSaleItem;
import com.liyun.promotion.service.IFlashSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/flash-sales")
@RequiredArgsConstructor
@Tag(name = "秒杀活动管理（管理员）")
public class FlashSaleAdminController {

    private final IFlashSaleService flashSaleService;

    @PostMapping
    @Operation(summary = "创建秒杀活动")
    public Result<Void> create(@RequestBody FlashSale flashSale,
                                @RequestParam List<FlashSaleItem> items) {
        flashSaleService.createFlashSale(flashSale, items);
        return Result.success();
    }

    @PutMapping("/{id}/start")
    @Operation(summary = "开始秒杀活动")
    public Result<Void> start(@PathVariable Long id) {
        FlashSale sale = flashSaleService.getById(id);
        if (sale == null) return Result.fail("活动不存在");
        sale.setStatus(2);
        flashSaleService.updateById(sale);
        return Result.success();
    }

    @PutMapping("/{id}/end")
    @Operation(summary = "结束秒杀活动")
    public Result<Void> end(@PathVariable Long id) {
        FlashSale sale = flashSaleService.getById(id);
        if (sale == null) return Result.fail("活动不存在");
        sale.setStatus(3);
        flashSaleService.updateById(sale);
        return Result.success();
    }
}
