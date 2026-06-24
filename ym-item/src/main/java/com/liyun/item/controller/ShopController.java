package com.liyun.item.controller;


import com.liyun.api.client.OrderFeign;
import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.dto.ShopEntryDTO;
import com.liyun.item.domain.dto.ShopSetupDTO;
import com.liyun.item.domain.pojo.Shop;
import com.liyun.item.domain.vo.ShopCartVO;
import com.liyun.item.domain.vo.ShopEntryVO;
import com.liyun.item.service.IShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shop")
@Tag(name = "商家管理", description = "商家管理接口")
@RequiredArgsConstructor
public class ShopController {
    private final IShopService shopService;
    private final OrderFeign orderFeign;

    @Operation(summary = "添加商家")
    @PostMapping("/register")
    public Result register(@RequestBody ShopDTO dto ,HttpServletRequest request){
        String ip = getClientIp(request);
        shopService.addShop(dto, ip);
        return Result.success();
    }

    @Operation(summary = "设置店铺信息")
    @PostMapping("/setup")
    public Result setupShop(@Valid @RequestBody ShopSetupDTO dto) {
        checkShopRole();
        Long userId = UserContext.getUserId();
        shopService.setupShop(userId, dto);
        return Result.success();
    }

    @Operation(summary = "查询当前用户店铺状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> shopStatus() {
        checkShopRole();
        return Result.success(shopService.getShopStatus());
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

    @Operation(summary = "上传店铺头像")
    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        checkShopRole();
        String avatarUrl = shopService.uploadShopAvatar(file);
        return Result.success(avatarUrl);
    }

    @Operation(summary = "获取单个店铺详情")
    @GetMapping("/{shopId}")
    public Result<Shop> getShopById(@PathVariable Long shopId) {
        Shop shop = shopService.getShopById(shopId);
        return Result.success(shop);
    }

    @Operation(summary = "获取当前商家仪表盘数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> shopStats() {
        checkShopRole();
        return Result.success(shopService.getShopStats());
    }

    @Operation(summary = "查询当前店铺订单列表")
    @GetMapping("/orders")
    public Result<Map<String, Object>> shopOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        checkShopRole();
        Long shopId = getCurrentShopId();
        if (shopId == null) return Result.fail("您还未开设店铺");
        return Result.success(orderFeign.getShopOrders(shopId, page, size, status, keyword));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/orders/{orderId}")
    public Result<Map<String, Object>> shopOrderDetail(@PathVariable String orderId) {
        checkShopRole();
        return Result.success(orderFeign.getShopOrderDetail(orderId));
    }

    @Operation(summary = "发货")
    @PutMapping("/orders/{orderId}/ship")
    public Result<Void> shipOrder(
            @PathVariable String orderId,
            @RequestParam String trackingNo) {
        checkShopRole();
        orderFeign.shipOrder(orderId, trackingNo);
        return Result.success();
    }

    @Operation(summary = "收入趋势")
    @GetMapping("/stats/revenue")
    public Result<Map<String, Object>> revenueTrend(
            @RequestParam(defaultValue = "7d") String period) {
        checkShopRole();
        Long shopId = getCurrentShopId();
        if (shopId == null) return Result.fail("您还未开设店铺");
        return Result.success(orderFeign.getShopRevenue(shopId, period));
    }

    @Operation(summary = "商家入驻（设置店铺信息）")
    @PostMapping("/entry")
    public Result<ShopEntryVO> entry(@Valid @RequestBody ShopEntryDTO dto) {
        checkShopRole();
        Long userId = UserContext.getUserId();
        ShopEntryVO vo = shopService.entryShop(userId, dto);
        return Result.success(vo);
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

    /**
     * 校验当前用户是否为商家角色
     */
    private void checkShopRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new RuntimeException("仅商家可访问此接口");
        }
    }

    /**
     * 从店铺状态中获取当前商家的 shopId
     */
    private Long getCurrentShopId() {
        Map<String, Object> status = shopService.getShopStatus();
        if (!(Boolean) status.get("hasShop")) {
            return null;
        }
        return ((Number) status.get("shopId")).longValue();
    }
}
