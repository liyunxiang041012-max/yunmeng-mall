package com.liyun.ai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IntentType {

    ORDER("订单", "查订单、买了什么、物流、发货、到哪了"),
    RECOMMEND("推荐", "推荐、有什么好的、帮我找、想买"),
    COUPON("优惠券", "优惠券、券、折扣、满减"),
    CART("购物车", "购物车、加了什么、结算"),
    SHOP("店铺", "店铺、商家、品牌"),
    CHAT("闲聊", "你好、谢谢、其他");

    private final String name;
    private final String keywords;
}
