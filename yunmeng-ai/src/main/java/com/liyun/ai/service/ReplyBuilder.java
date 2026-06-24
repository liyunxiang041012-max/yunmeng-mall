package com.liyun.ai.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ReplyBuilder {

    /** 组装订单回复 */
    @SuppressWarnings("unchecked")
    public String buildOrderReply(Object result) {
        if (result == null) {
            return "暂时无法查询订单，请稍后重试";
        }
        if (!(result instanceof Map)) {
            return "查询到您的订单数据，但格式异常，请在订单页面查看";
        }

        Map<String, Object> resultMap = (Map<String, Object>) result;
        Object data = resultMap.get("data");
        if (data instanceof List) {
            List<?> rawList = (List<?>) data;
            if (rawList.isEmpty()) {
                return "您还没有任何订单哦，快去商城逛逛吧";
            }
            StringBuilder sb = new StringBuilder("您最近的订单：\n");
            int count = 0;
            for (Object o : rawList) {
                if (o instanceof Map) {
                    Map<String, Object> order = (Map<String, Object>) o;
                    String status = getOrderStatus(String.valueOf(order.getOrDefault("status", "")));
                    Object amount = order.get("payAmount");
                    String amountStr = amount != null ? (Long.parseLong(String.valueOf(amount)) / 100.0) + "元" : "金额未知";
                    sb.append("\n").append(++count).append(". 订单 ").append(order.getOrDefault("id", "未知"))
                            .append(" | ").append(status)
                            .append(" | ").append(amountStr);
                    if (count >= 5) break;
                }
            }
            return sb.toString();
        }

        return "查询到您的订单数据，但格式异常，请在订单页面查看";
    }

    /** 组装优惠券回复 */
    @SuppressWarnings("unchecked")
    public String buildCouponReply(Object result) {
        if (result == null) {
            return "暂时无法查询优惠券，请稍后重试";
        }
        if (!(result instanceof Map)) {
            return "查询到您的优惠券数据，但格式异常，请在优惠券页面查看";
        }

        // ym-promotion 返回的是 PageDTO，不存在 code 字段，跳过 code 校验
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Object data = resultMap.get("code") != null ? resultMap.get("data") : resultMap;
        if (data instanceof Map) {
            Map<String, Object> page = (Map<String, Object>) data;
            Object records = page.get("list");
            if (records instanceof List) {
                List<?> coupons = (List<?>) records;
                if (coupons.isEmpty()) {
                    return "您当前没有可用的优惠券。去逛逛领券中心吧";
                }

                StringBuilder sb = new StringBuilder("您有 ").append(coupons.size()).append(" 张可用优惠券：\n");
                int count = 0;
                for (Object o : coupons) {
                    if (o instanceof Map) {
                        Map<String, Object> c = (Map<String, Object>) o;
                        sb.append("\n").append(++count).append(". ")
                                .append(c.getOrDefault("name", "优惠券"));
                        Object endTime = c.get("termEndTime");
                        if (endTime != null) {
                            sb.append("（有效期至 ").append(endTime).append("）");
                        }
                        if (count >= 5) break;
                    }
                }
                return sb.toString();
            }
        }

        return "查询到您的优惠券数据，但格式异常，请在优惠券页面查看";
    }

    /** 组装购物车回复 */
    @SuppressWarnings("unchecked")
    public String buildCartReply(Object result) {
        if (result == null) {
            return "暂时无法查询购物车，请稍后重试";
        }
        if (!(result instanceof Map)) {
            return "查询到您的购物车数据，但格式异常，请在购物车页面查看";
        }

        Map<String, Object> resultMap = (Map<String, Object>) result;
        Object data = resultMap.get("data");
        if (data instanceof List) {
            List<?> rawList = (List<?>) data;
            if (rawList.isEmpty()) {
                return "您的购物车是空的，快去逛逛添加商品吧";
            }

            long total = 0;
            StringBuilder sb = new StringBuilder("您的购物车有 ").append(rawList.size()).append(" 件商品：\n");
            int count = 0;
            for (Object o : rawList) {
                if (o instanceof Map) {
                    Map<String, Object> item = (Map<String, Object>) o;
                    Object price = item.get("price");
                    total += price != null ? Long.parseLong(String.valueOf(price)) : 0;
                    sb.append("\n").append(++count).append(". ").append(item.getOrDefault("skuName", "商品"));
                    if (count >= 5) {
                        sb.append("\n... 还有 ").append(rawList.size() - 5).append(" 件");
                        break;
                    }
                }
            }
            sb.append("\n\n合计 ").append(String.format("%.2f", total / 100.0)).append(" 元");
            return sb.toString();
        }

        return "查询到您的购物车数据，但格式异常，请在购物车页面查看";
    }

    /** 组装推荐回复 */
    @SuppressWarnings("unchecked")
    public String buildRecommendReply(Object result) {
        if (result == null) {
            return "暂时无法查询商品，请稍后重试";
        }
        if (!(result instanceof Map)) {
            return "查询到商品数据，但格式异常，请直接搜索";
        }

        Map<String, Object> resultMap = (Map<String, Object>) result;
        if (!"200".equals(String.valueOf(resultMap.get("code")))) {
            return "暂时无法查询商品，请稍后重试";
        }

        Object data = resultMap.get("data");
        if (data instanceof Map) {
            Map<String, Object> page = (Map<String, Object>) data;
            Object records = page.get("list");
            if (records instanceof List) {
                List<?> items = (List<?>) records;
                if (items.isEmpty()) {
                    return "抱歉，没有找到相关商品，换个关键词试试吧";
                }

                StringBuilder sb = new StringBuilder("为您找到以下商品：\n");
                int count = 0;
                for (Object o : items) {
                    if (o instanceof Map) {
                        Map<String, Object> item = (Map<String, Object>) o;
                        String name = String.valueOf(item.getOrDefault("name", "商品"));
                        Object price = item.get("price");
                        String priceStr = price != null
                                ? String.format("%.2f元", Long.parseLong(String.valueOf(price)) / 100.0)
                                : "价格未知";
                        sb.append("\n").append(++count).append(". ").append(name)
                                .append(" | ").append(priceStr);
                        if (count >= 5) break;
                    }
                }
                return sb.toString();
            }
        }

        return "查询到商品数据，但格式异常，请直接搜索";
    }

    /** 闲聊降级回复 */
    public String buildChatReply(String message) {
        String msg = message != null ? message.toLowerCase().trim() : "";
        if (msg.contains("你好") || msg.contains("hi") || msg.contains("hello")) {
            return "你好！我是云梦 AI 助手，可以帮助你：\n- 查订单 - 试试说\"我的订单\"\n- 查优惠券 - 试试说\"我有什么券\"\n- 查购物车 - 试试说\"我的购物车\"\n- 推荐商品 - 试试说\"推荐一款耳机\"";
        }
        if (msg.contains("谢谢") || msg.contains("感谢") || msg.contains("thank")) {
            return "不客气！还有什么可以帮你的吗？";
        }
        if (msg.contains("再见") || msg.contains("拜拜")) {
            return "再见，祝您购物愉快！";
        }
        return "抱歉，我还不太理解你的意思。你可以试试问我：\n- 帮我查最近订单\n- 推荐一款耳机\n- 我有什么优惠券\n- 我的购物车";
    }

    private String getOrderStatus(String status) {
        switch (status) {
            case "PENDING_PAYMENT":
            case "0": return "待付款";
            case "PAID":
            case "1": return "已付款";
            case "SHIPPED":
            case "2": return "已发货";
            case "COMPLETED":
            case "3": return "已完成";
            case "CANCELLED":
            case "4": return "已取消";
            case "REFUNDED":
            case "5": return "已退款";
            default: return status;
        }
    }
}
