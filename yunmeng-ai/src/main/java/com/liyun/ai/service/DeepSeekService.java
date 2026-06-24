package com.liyun.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyun.ai.config.DeepSeekConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final DeepSeekConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 调用 DeepSeek 生成回复
     *
     * @param systemPrompt 系统提示词（包含业务数据）
     * @param userMessage  用户消息
     * @param history      多轮对话历史，可为 null
     * @return AI 回复文本，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userMessage, List<Map<String, String>> history) {
        try {
            String url = config.getBaseUrl() + "/v1/chat/completions";

            // 构建 messages
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : getDefaultSystemPrompt()));

            // 最近 5 轮对话历史
            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 10);
                for (int i = start; i < history.size(); i++) {
                    messages.add(history.get(i));
                }
            }

            messages.add(Map.of("role", "user", "content", userMessage));

            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel());
            body.put("messages", messages);
            body.put("temperature", config.getTemperature());
            body.put("max_tokens", config.getMaxTokens());

            // HTTP 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }

            log.warn("DeepSeek 返回格式异常: {}", response.getBody());
            return null;

        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            return null;
        }
    }

    private String getDefaultSystemPrompt() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE));
        return """
                你是云梦商城的 AI 智能客服助手，名字叫"云梦 AI 助手"。
                今天是%s。
                你的职责是帮助用户查询订单、优惠券、购物车、推荐商品等。
                请根据提供的业务数据，用友好、自然的语气回复用户。
                
                回复要求：
                - 用中文回复
                - 简洁明了，不要长篇大论
                - 如果数据为空，友好地提示用户
                - 如果用户问的是无关问题，引导用户使用商城功能
                """.formatted(today);
    }
}
