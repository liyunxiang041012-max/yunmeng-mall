package com.liyun.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** 角色：user / ai */
    private String role;

    /** 消息内容 */
    private String text;

    /** 消息时间 */
    private String time;
}
