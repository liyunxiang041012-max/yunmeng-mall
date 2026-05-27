package com.liyun.item.controller;


import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.vo.ShopCartVO;
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
