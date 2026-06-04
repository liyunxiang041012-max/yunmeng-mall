package com.liyun.item.controller;


import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.vo.FollowShopVO;
import com.liyun.item.domain.vo.ItemVO;
import com.liyun.item.domain.vo.ShopCartVO;
import com.liyun.item.domain.vo.ShopDetailVO;
import com.liyun.item.service.IShopFollowService;
import com.liyun.item.service.IShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shop")
@Tag(name = "商家管理", description = "商家管理接口")
@RequiredArgsConstructor
public class ShopController {
    private final IShopService shopService;
    private final IShopFollowService shopFollowService;

    @Operation(summary = "添加商家")
    @PostMapping("/register")
    public Result register(@RequestBody ShopDTO dto ,HttpServletRequest request){
        String ip = getClientIp(request);
        shopService.addShop(dto, ip);
        return Result.success();
    }

    @Operation(summary = "获取商家信息")
    @GetMapping("/cart/{id}")
    public ShopCartVO getShop(@PathVariable Long id) {
        return shopService.getCartShopInfo(id);
    }

    @Operation(summary = "批量获取商家信息")
    @PostMapping("/batch-info")
    public List<ShopCartVO> batchGetShop(@RequestBody List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Collections.emptyList();
        }
        return shopService.batchGetCartShopInfo(shopIds);
    }

    @Operation(summary = "获取商家详细信息")
    @GetMapping("/info/{shopId}")
    public ShopInfoDTO getShopInfo(@PathVariable("shopId") Long shopId) {
        ShopInfoDTO dto = shopService.getShopInfo(shopId);
        if (dto == null) {
            throw new RuntimeException("店铺不存在");
        }
        return dto;
    }

    @Operation(summary = "批量获取商家详细信息")
    @PostMapping("/batch-detail")
    public List<ShopInfoDTO> batchGetShopInfo(@RequestBody List<Long> shopIds) {
        return shopService.batchGetShopInfo(shopIds);
    }

    @Operation(summary = "获取店铺详情")
    @GetMapping("/{shopId}")
    public Result<ShopDetailVO> getShopDetail(@PathVariable Long shopId) {
        return Result.success(shopService.getShopDetail(shopId));
    }

    @Operation(summary = "获取店铺商品列表")
    @GetMapping("/{shopId}/items")
    public Result<PageDTO<ItemVO>> getShopItems(
            @PathVariable Long shopId,
            @RequestParam(required = false, defaultValue = "default") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.success(shopService.getShopItems(shopId, sort, page, size));
    }

    @Operation(summary = "切换关注店铺")
    @PostMapping("/follow/toggle/{shopId}")
    public Result<Boolean> toggleFollow(@PathVariable Long shopId) {
        Long userId = UserContext.getUserId();
        boolean followed = shopFollowService.toggleFollow(userId, shopId);
        return Result.success(followed);
    }

    @Operation(summary = "检查关注状态")
    @GetMapping("/follow/check/{shopId}")
    public Result<Boolean> checkFollow(@PathVariable Long shopId) {
        Long userId = UserContext.getUserId();
        boolean followed = shopFollowService.checkFollow(userId, shopId);
        return Result.success(followed);
    }

    @Operation(summary = "获取我的关注列表")
    @GetMapping("/follow/my")
    public Result<List<FollowShopVO>> getMyFollows() {
        Long userId = UserContext.getUserId();
        return Result.success(shopFollowService.getMyFollows(userId));
    }


    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }


}
