package com.liyun.user.controller;

import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.user.service.IAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员后端 — 用户管理 CRUD
 */
@RestController
@RequestMapping("/user/admin")
@Tag(name = "管理员用户管理", description = "管理员对用户的增删改查")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public Result<PageDTO<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.pageUsers(page, size, keyword, role, status));
    }

    @Operation(summary = "查询单个用户详情")
    @GetMapping("/{userId}")
    public Result<Map<String, Object>> detail(@PathVariable Long userId) {
        Map<String, Object> detail = adminService.getUserDetail(userId);
        if (detail == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(detail);
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{userId}/status")
    public Result<Void> toggleStatus(@PathVariable Long userId, @RequestBody Map<String, Integer> body) {
        Integer st = body.get("status");
        if (st == null || (st != 0 && st != 1)) {
            return Result.fail("status 必须为 0(禁用) 或 1(启用)");
        }
        adminService.toggleUserStatus(userId, st);
        return Result.success();
    }

    @Operation(summary = "修改用户角色")
    @PutMapping("/{userId}/role")
    public Result<Void> changeRole(@PathVariable Long userId, @RequestBody Map<String, Integer> body) {
        Integer r = body.get("role");
        if (r == null || r < 0 || r > 2) {
            return Result.fail("role 必须为 0(用户) / 1(商家) / 2(管理员)");
        }
        adminService.changeUserRole(userId, r);
        return Result.success();
    }

    @Operation(summary = "管理后台仪表盘概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(adminService.getOverview());
    }

    @Operation(summary = "管理后台收入趋势")
    @GetMapping("/revenue")
    public Result<Map<String, Object>> revenue(@RequestParam(defaultValue = "7") int period) {
        return Result.success(adminService.getRevenue(period));
    }
}
