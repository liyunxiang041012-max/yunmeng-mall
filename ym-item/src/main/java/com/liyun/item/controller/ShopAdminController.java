package com.liyun.item.controller;

import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.pojo.Shop;
import com.liyun.item.service.IShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商家管理后台 - 增删改查
 */
@RestController
@RequestMapping("/shop/admin")
@Tag(name = "商家管理后台", description = "管理员对商家的增删改查")
@RequiredArgsConstructor
public class ShopAdminController {

    private final IShopService shopService;

    @Operation(summary = "分页查询商家列表")
    @GetMapping("/page")
    public Result<PageDTO<Shop>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(shopService.listShops(page, size, keyword, status));
    }

    @Operation(summary = "查询单个商家详情")
    @GetMapping("/{shopId}")
    public Result<Shop> detail(@PathVariable Long shopId) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) return Result.fail("店铺不存在");
        return Result.success(shop);
    }

    @Operation(summary = "编辑商家信息")
    @PutMapping("/{shopId}")
    public Result edit(@PathVariable Long shopId, @RequestBody Map<String, String> body) {
        shopService.updateShopInfo(shopId,
                body.get("shopName"),
                body.get("logo"),
                body.get("description"));
        return Result.success();
    }

    @Operation(summary = "删除商家（软删除）")
    @DeleteMapping("/{shopId}")
    public Result delete(@PathVariable Long shopId) {
        shopService.deleteShop(shopId);
        return Result.success();
    }

    @Operation(summary = "开关商家状态（营业/关闭）")
    @PutMapping("/toggle-status/{shopId}")
    public Result<Shop> toggleStatus(@PathVariable Long shopId) {
        shopService.toggleShopStatus(shopId);
        Shop updated = shopService.getById(shopId);
        return Result.success(updated);
    }
}
