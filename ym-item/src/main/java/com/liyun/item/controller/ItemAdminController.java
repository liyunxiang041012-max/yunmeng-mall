package com.liyun.item.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.service.IItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员后台 - 商品审核
 */
@RestController
@RequestMapping("/shop/admin/item")
@Tag(name = "管理员商品审核", description = "管理员审核商家提交的商品")
@RequiredArgsConstructor
public class ItemAdminController {

    private final IItemService itemService;

    @Operation(summary = "分页查询所有商品（含店铺名，支持 status 和 auditStatus 筛选）")
    @GetMapping("/page")
    public Result<PageDTO<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword) {
        checkAdminRole();
        return Result.success(itemService.listAllItems(page, size, status, auditStatus, keyword));
    }

    @Operation(summary = "查看单个商品详情")
    @GetMapping("/{itemId}")
    public Result<Item> detail(@PathVariable Long itemId) {
        checkAdminRole();
        Item item = itemService.getById(itemId);
        if (item == null) return Result.fail("商品不存在");
        return Result.success(item);
    }

    @Operation(summary = "审核通过 → auditStatus=1")
    @PutMapping("/{itemId}/approve")
    public Result<Void> approve(@PathVariable Long itemId) {
        checkAdminRole();
        itemService.approveItem(itemId);
        return Result.success();
    }

    @Operation(summary = "审核驳回 → auditStatus=2")
    @PutMapping("/{itemId}/reject")
    public Result<Void> reject(@PathVariable Long itemId) {
        checkAdminRole();
        itemService.rejectItem(itemId);
        return Result.success();
    }

    @Operation(summary = "管理员上下架商品（仅 auditStatus=1 可操作）")
    @PutMapping("/toggle-status/{itemId}")
    public Result<Void> toggleStatus(@PathVariable Long itemId) {
        checkAdminRole();
        itemService.adminToggleStatus(itemId);
        return Result.success();
    }

    private void checkAdminRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new RuntimeException("仅管理员可访问此接口");
        }
    }
}
