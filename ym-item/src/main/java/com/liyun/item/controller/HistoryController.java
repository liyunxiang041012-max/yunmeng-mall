package com.liyun.item.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.vo.HistoryItemVO;
import com.liyun.item.service.IHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 浏览历史 前端控制器
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
@RestController
@RequestMapping("/history")
@Tag(name = "浏览历史", description = "浏览历史接口")
@RequiredArgsConstructor
public class HistoryController {

    private final IHistoryService historyService;

    @Operation(summary = "获取我的浏览历史")
    @GetMapping("/my")
    public Result<PageDTO<HistoryItemVO>> getMyHistory(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(historyService.getMyHistory(userId, page, size));
    }

    @Operation(summary = "删除单条浏览记录")
    @DeleteMapping("/{id}")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        historyService.deleteHistory(userId, id);
        return Result.success();
    }

    @Operation(summary = "清空全部浏览记录")
    @DeleteMapping("/clear")
    public Result<Void> clearHistory() {
        Long userId = UserContext.getUserId();
        historyService.clearHistory(userId);
        return Result.success();
    }
}
