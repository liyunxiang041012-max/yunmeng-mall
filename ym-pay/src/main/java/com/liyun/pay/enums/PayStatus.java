package com.liyun.pay.enums;

import lombok.Getter;

@Getter
public enum PayStatus {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款"),
    CLOSED(3, "已关闭");

    private final int code;
    private final String desc;

    PayStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PayStatus of(int code) {
        for (PayStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知支付状态: " + code);
    }
}