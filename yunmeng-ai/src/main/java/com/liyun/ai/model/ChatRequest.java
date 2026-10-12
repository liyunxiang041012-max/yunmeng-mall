package com.liyun.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** 用户消息 */
    private String message;

    /** 会话ID（可选，传空则创建新会话） */
    private String sessionId;
}
