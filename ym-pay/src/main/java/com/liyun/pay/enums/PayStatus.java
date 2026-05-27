package com.liyun.pay.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PayStatus {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    FAILED(2, "已失败"),
    REFUNDED(3, "已退款"),
    CANCELLED(4, "已取消");

    @EnumValue
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