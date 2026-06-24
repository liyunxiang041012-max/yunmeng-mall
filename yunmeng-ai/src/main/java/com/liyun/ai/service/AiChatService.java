package com.liyun.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyun.ai.model.ChatHistory;
import com.liyun.ai.model.ChatMessage;
import com.liyun.ai.model.ChatResponse;
import com.liyun.ai.model.IntentType;
import com.liyun.api.client.CartFeign;
import com.liyun.api.client.ItemFeign;
import com.liyun.api.client.OrderFeign;
import com.liyun.api.client.PromotionFeign;
import com.liyun.api.client.ShopFeign;
import com.liyun.common.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final IntentParser intentParser;
    private final ReplyBuilder replyBuilder;
    private final DeepSeekService deepSeekService;
    private final OrderFeign orderFeign;
    private final ItemFeign itemFeign;
    private final PromotionFeign promotionFeign;
    private final CartFeign cartFeign;
    private final ShopFeign shopFeign;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String HISTORY_KEY_PREFIX = "chat:history:";
    private static final int HISTORY_TTL_HOURS = 24;

    /**
     * 处理用户消息，返回 AI 回复
     */
    public ChatResponse handleMessage(String message, String sessionId) {
        // 1. 生成或复用 sessionId
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "ai_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
        }

        // 2. 意图识别（规则匹配，快速免费）
        IntentType intent = intentParser.parse(message);
        log.info("AI 意图识别: message={}, intent={}", message, intent);

        // 2.5 业务意图需要登录，提前校验
        if (isBusinessIntent(intent) && UserContext.getUserId() == null) {
            String reply = "要帮你查" + intent.getName() + "的话，需要先登录哦！登录后我就能帮你查看啦～";
            saveMessage(sessionId, "user", message);
            saveMessage(sessionId, "ai", reply);
            return new ChatResponse(reply, sessionId);
        }

        // 3. 根据意图获取业务数据
        Object data = null;
        String dataContext = "";
        try {
            switch (intent) {
                case ORDER -> {
                    data = orderFeign.getUserOrders();
                    log.info("Feign 返回订单原始数据: {}", data);
                    dataContext = buildOrderContext((Map<String, Object>) data);
                }
                case COUPON -> {
                    data = promotionFeign.pageQueryUserCoupons(1);
                    dataContext = buildCouponContext((Map<String, Object>) data);
                }
                case CART -> {
                    data = cartFeign.getUserCart();
                    dataContext = buildCartContext((Map<String, Object>) data);
                }
                case RECOMMEND -> {
                    String keyword = extractKeyword(message);
                    data = itemFeign.searchItems(keyword);
                    dataContext = buildRecommendContext((Map<String, Object>) data, keyword);
                }
            }
        } catch (Exception e) {
            log.error("获取业务数据失败 intent={}", intent, e);
        }

        // 4. 加载对话历史（最近 10 条）
        List<Map<String, String>> history = loadHistory(sessionId);

        // 5. 调用 DeepSeek 生成回复，失败降级到模板
        String reply;
        try {
            String systemPrompt = buildSystemPrompt(intent, dataContext);
            reply = deepSeekService.chat(systemPrompt, message, history);
            if (reply == null || reply.isBlank()) {
                // DeepSeek 失败，降级到规则模板
                log.info("DeepSeek 返回空，降级到规则模板 intent={}", intent);
                reply = buildFallbackReply(intent, data, message);
            }
        } catch (Exception e) {
            log.error("DeepSeek 调用异常，降级到规则模板 intent={}", intent, e);
            reply = buildFallbackReply(intent, data, message);
        }

        // 6. 保存消息到历史（先存用户消息，再存 AI 回复）
        saveMessage(sessionId, "user", message);
        saveMessage(sessionId, "ai", reply);

        return new ChatResponse(reply, sessionId);
    }

    // ==================== 历史管理 ====================

    public ChatHistory getHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        String key = HISTORY_KEY_PREFIX + sessionId;
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonList == null || jsonList.isEmpty()) return null;
        List<ChatMessage> messages = jsonList.stream()
                .map(json -> {
                    try { return objectMapper.readValue(json, ChatMessage.class); }
                    catch (JsonProcessingException e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();
        return new ChatHistory(sessionId, messages);
    }

    public void clearHistory(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            redisTemplate.delete(HISTORY_KEY_PREFIX + sessionId);
        }
    }

    private List<Map<String, String>> loadHistory(String sessionId) {
        String key = HISTORY_KEY_PREFIX + sessionId;
        List<String> jsonList = redisTemplate.opsForList().range(key, -10, -1);
        if (jsonList == null || jsonList.isEmpty()) return Collections.emptyList();
        return jsonList.stream()
                .map(json -> {
                    try {
                        ChatMessage msg = objectMapper.readValue(json, ChatMessage.class);
                        // DeepSeek API 要求 assistant 而非 ai
                        String role = "ai".equals(msg.getRole()) ? "assistant" : msg.getRole();
                        return Map.of("role", role, "content", msg.getText());
                    } catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void saveMessage(String sessionId, String role, String text) {
        try {
            ChatMessage msg = new ChatMessage(role, text,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            String json = objectMapper.writeValueAsString(msg);
            String key = HISTORY_KEY_PREFIX + sessionId;
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, HISTORY_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("保存消息到 Redis 失败", e);
        }
    }

    // ==================== 数据上下文构建 ====================

    @SuppressWarnings("unchecked")
    private String buildOrderContext(Map<String, Object> result) {
        if (result == null) return "";
        Object data = result.get("data");
        if (data instanceof List) {
            List<Map<String, Object>> orders = (List<Map<String, Object>>) data;
            if (orders.isEmpty()) return "用户当前没有订单。";
            StringBuilder sb = new StringBuilder("用户订单数据：\n");
            int i = 1;
            for (Map<String, Object> o : orders) {
                if (i > 10) { sb.append("... 共 ").append(orders.size()).append(" 条订单\n"); break; }
                Object amount = o.get("payAmount");
                String amountStr = amount != null ? (Long.parseLong(String.valueOf(amount)) / 100.0) + "元" : "未知";
                sb.append(i++).append(". 订单号").append(o.getOrDefault("id", "")).append(" 状态").append(o.getOrDefault("status", "")).append(" 实付").append(amountStr).append("\n");
            }
            return sb.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String buildCouponContext(Map<String, Object> result) {
        if (result == null) return "";
        Object couponsObj = result.get("list");
        if (couponsObj instanceof List) {
            List<Map<String, Object>> coupons = (List<Map<String, Object>>) couponsObj;
            if (coupons.isEmpty()) return "用户当前没有可用优惠券。";
            StringBuilder sb = new StringBuilder("用户优惠券数据（共" + coupons.size() + "张）：\n");
            int i = 1;
            for (Map<String, Object> c : coupons) {
                if (i > 10) { sb.append("... 共 ").append(coupons.size()).append(" 张优惠券\n"); break; }
                sb.append(i++).append(". ").append(c.getOrDefault("name", "优惠券"));
                Object end = c.get("termEndTime");
                if (end != null) sb.append(" 有效期至").append(end);
                sb.append("\n");
            }
            return sb.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String buildCartContext(Map<String, Object> result) {
        if (result == null) return "";
        Object data = result.get("data");
        if (data instanceof List) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) data;
            if (items.isEmpty()) return "用户购物车是空的。";
            long total = 0;
            StringBuilder sb = new StringBuilder("用户购物车数据（共" + items.size() + "件）：\n");
            int i = 1;
            for (Map<String, Object> item : items) {
                if (i > 10) { sb.append("... 共 ").append(items.size()).append(" 件商品\n"); break; }
                Object price = item.get("price");
                total += price != null ? Long.parseLong(String.valueOf(price)) : 0;
                sb.append(i++).append(". ").append(item.getOrDefault("skuName", "商品")).append("\n");
            }
            sb.append("合计约").append(String.format("%.2f", total / 100.0)).append("元\n");
            return sb.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String buildRecommendContext(Map<String, Object> result, String keyword) {
        if (result == null) return "搜索关键词：" + keyword;
        Object data = result.get("data");
        if (data instanceof Map) {
            Object list = ((Map<String, Object>) data).get("list");
            if (list instanceof List) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) list;
                if (items.isEmpty()) return "搜索关键词：" + keyword + "，未找到匹配商品。";
                StringBuilder sb = new StringBuilder("搜索关键词：" + keyword + "，匹配商品：\n");
                int i = 1;
                for (Map<String, Object> item : items) {
                    if (i > 10) { sb.append("... 共 ").append(items.size()).append(" 件商品\n"); break; }
                    String name = String.valueOf(item.getOrDefault("name", "商品"));
                    Object price = item.get("price");
                    String priceStr = price != null ? String.format("%.2f元", Long.parseLong(String.valueOf(price)) / 100.0) : "未知";
                    sb.append(i++).append(". ").append(name).append(" — ").append(priceStr).append("\n");
                }
                return sb.toString();
            }
        }
        return "搜索关键词：" + keyword;
    }

    // ==================== 系统提示词 ====================

    private String buildSystemPrompt(IntentType intent, String dataContext) {
        String intentGuide = switch (intent) {
            case ORDER -> "用户想查询订单。请根据下方订单数据，用友好的语气帮用户总结订单情况。";
            case COUPON -> "用户想查询优惠券。请根据下方优惠券数据，告诉用户有哪些可用的券。";
            case CART -> "用户想查看购物车。请根据下方购物车数据，帮用户梳理购物车内容。";
            case RECOMMEND -> "用户想搜索/推荐商品。请根据下方商品数据，帮用户推荐和介绍商品。";
            case SHOP -> "用户想查询店铺信息。引导用户在商城首页浏览店铺。";
            default -> "用户正在闲聊或询问。请友好地回复，并引导用户使用商城功能（查订单、查优惠券、看购物车、搜商品等）。";
        };

        // 数据为空时的明确告知，防止 AI 胡编
        boolean hasData = !dataContext.isBlank();
        String dataInfo;
        if (hasData) {
            dataInfo = "【业务数据】\n" + dataContext;
        } else if (intent == IntentType.CHAT) {
            dataInfo = "";
        } else {
            dataInfo = "【重要】暂时无法获取用户的" + intent.getName() + "数据（可能是用户未登录或服务繁忙）。请友好地告知用户当前无法查询，建议稍后重试或登录后查看。不要编造任何具体数据，不要问用户要手机号、验证码等隐私信息。";
        }

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE));
        return """
                你是云梦商城的 AI 智能客服助手，名字叫%1$s。
                今天是%4$s。
                
                %2$s
                
                %3$s
                
                回复要求：
                - 用中文回复，语气友好自然
                - 简洁明了，不要长篇大论（200字以内）
                - 如果数据为空，友好地提示用户
                - 可以适当推荐商城其他功能
                - 不要编造数据
                - 绝不要索取用户的手机号、密码、验证码等隐私信息
                """.formatted("云梦 AI 助手", intentGuide, dataInfo, today);
    }

    // ==================== 降级回复 ====================

    private String buildFallbackReply(IntentType intent, Object data, String message) {
        return switch (intent) {
            case ORDER -> replyBuilder.buildOrderReply(data);
            case COUPON -> replyBuilder.buildCouponReply(data);
            case CART -> replyBuilder.buildCartReply(data);
            case RECOMMEND -> replyBuilder.buildRecommendReply(data);
            case SHOP -> "抱歉，店铺查询功能还在开发中。您可以在商城首页浏览所有店铺哦！";
            default -> replyBuilder.buildChatReply(message);
        };
    }

    private String extractKeyword(String message) {
        if (message == null) return "";
        return message.replaceAll("推荐|有什么|帮我找|想买|买什么|搜索|搜", "").trim();
    }

    /** 判断是否需要登录才能获取数据的意图 */
    private boolean isBusinessIntent(IntentType intent) {
        return intent == IntentType.ORDER
                || intent == IntentType.COUPON
                || intent == IntentType.CART;
    }
}
