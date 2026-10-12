package com.liyun.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** AI 回复内容 */
    private String reply;

    /** 会话 ID */
    private String sessionId;
}
