package com.liyun.ai.controller;

import com.liyun.ai.model.ChatHistory;
import com.liyun.ai.model.ChatRequest;
import com.liyun.ai.model.ChatResponse;
import com.liyun.ai.service.AiChatService;
import com.liyun.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@Tag(name = "AI 聊天", description = "AI 智能客服接口")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @Operation(summary = "发送消息")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = aiChatService.handleMessage(request.getMessage(), request.getSessionId());
        return Result.success(response);
    }

    @Operation(summary = "获取对话历史")
    @GetMapping("/history")
    public Result<ChatHistory> getHistory(@RequestParam String sessionId) {
        ChatHistory history = aiChatService.getHistory(sessionId);
        if (history == null) {
            return Result.fail("会话不存在或已过期");
        }
        return Result.success(history);
    }

    @Operation(summary = "清除对话历史")
    @DeleteMapping({"/history/clear", "/history"})
    public Result<Void> clearHistory(@RequestParam String sessionId) {
        aiChatService.clearHistory(sessionId);
        return Result.success();
    }
}
