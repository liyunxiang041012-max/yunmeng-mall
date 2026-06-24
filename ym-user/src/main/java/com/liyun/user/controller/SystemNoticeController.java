package com.liyun.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.Result;
import com.liyun.user.domain.dto.NoticeSaveDTO;
import com.liyun.user.service.ISystemNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统通知 - 管理员发送 + 用户查看
 */
@RestController
@RequestMapping("/user")
@Tag(name = "系统通知", description = "管理员发送通知，用户查看通知")
@RequiredArgsConstructor
public class SystemNoticeController {

    private final ISystemNoticeService systemNoticeService;

    // ==================== 管理员 ====================

    @Operation(summary = "管理员发送系统通知")
    @PostMapping("/admin/notice")
    public Result<Void> send(@Valid @RequestBody NoticeSaveDTO dto) {
        checkAdminRole();
        Long adminId = UserContext.getUserId();
        systemNoticeService.sendNotice(dto, adminId);
        return Result.success();
    }

    // ==================== 用户 ====================

    @Operation(summary = "用户查询通知列表")
    @GetMapping("/notice/page")
    public Result<Page<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.success(systemNoticeService.pageUserNotices(userId, page, size));
    }

    @Operation(summary = "用户标记已读")
    @PutMapping("/notice/{noticeId}/read")
    public Result<Void> markRead(@PathVariable Long noticeId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        systemNoticeService.markRead(userId, noticeId);
        return Result.success();
    }

    @Operation(summary = "用户未读通知数量")
    @GetMapping("/notice/unread")
    public Result<Long> unreadCount() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.success(systemNoticeService.unreadCount(userId));
    }

    private void checkAdminRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new RuntimeException("仅管理员可访问此接口");
        }
    }
}
