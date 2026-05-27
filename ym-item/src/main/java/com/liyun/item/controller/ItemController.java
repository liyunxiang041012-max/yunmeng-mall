package com.liyun.item.controller;



import com.liyun.api.dto.ItemInfoDTO;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.vo.ItemDetailVO;
import com.liyun.item.domain.vo.ItemVO;
import com.liyun.item.query.ItemPageQuery;
import com.liyun.item.service.IItemDetailService;
import com.liyun.item.service.IItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品SPU 前端控制器
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/items")
@Tag(name = "商品查询", description = "商品查询接口")
@RequiredArgsConstructor
public class ItemController {
    private final IItemService itemService;
    private final IItemDetailService itemDetailService;
    @Operation(summary = "商品分页查询")
    @GetMapping("/page")
    public Result<PageDTO<ItemVO>> page(ItemPageQuery query) {
        return Result.success(itemService.pageQuery(query));
    }
    @GetMapping("/sync")
    public Result<Void> sync() {
        itemService.syncToEs();
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情查询")
    public Result<ItemDetailVO> getItemDetailById(@PathVariable Long id){
        return Result.success(itemDetailService.getItemDetail(id));
    }

    @GetMapping("/info/{itemId}")
    @Operation(summary = "获取商品基本信息")
    public Result<ItemInfoDTO> getItemInfo(@PathVariable("itemId") Long itemId) {
        ItemInfoDTO dto = itemService.getItemInfo(itemId);
        if (dto == null) {
            return Result.fail("商品不存在");
        }
        return Result.success(dto);
    }

    @PostMapping("/batch-info")
    @Operation(summary = "批量获取商品基本信息")
    public Result<List<ItemInfoDTO>> batchGetItemInfo(@RequestBody List<Long> itemIds) {
        List<ItemInfoDTO> result = itemService.batchGetItemInfo(itemIds);
        return Result.success(result);
    }


}
