package com.liyun.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {

    private String sessionId;

    private List<ChatMessage> messages;
}
