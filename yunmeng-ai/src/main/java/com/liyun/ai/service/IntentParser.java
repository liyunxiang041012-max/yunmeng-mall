package com.liyun.ai.service;

import com.liyun.ai.model.IntentType;
import org.springframework.stereotype.Component;

@Component
public class IntentParser {

    public IntentType parse(String message) {
        if (message == null || message.isBlank()) {
            return IntentType.CHAT;
        }

        String msg = message.toLowerCase();

        // 订单
        if (containsAny(msg, "订单", "买了什么", "物流", "发货", "到哪", "包裹", "快递", "我的单")) {
            return IntentType.ORDER;
        }

        // 优惠券
        if (containsAny(msg, "优惠券", "券", "折扣", "满减", "红包")) {
            return IntentType.COUPON;
        }

        // 购物车
        if (containsAny(msg, "购物车", "加了什么", "结算", "车", "cart")) {
            return IntentType.CART;
        }

        // 推荐商品
        if (containsAny(msg, "推荐", "有什么好", "帮我找", "想买", "买什么", "搜", "找一")) {
            return IntentType.RECOMMEND;
        }

        // 店铺
        if (containsAny(msg, "店铺", "商家", "品牌", "店家", "店")) {
            return IntentType.SHOP;
        }

        // 兜底闲聊
        return IntentType.CHAT;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
