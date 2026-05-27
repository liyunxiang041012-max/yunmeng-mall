package com.liyun.pay.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING_PAYMENT(0, "待付款"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDED(5, "已退款");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}