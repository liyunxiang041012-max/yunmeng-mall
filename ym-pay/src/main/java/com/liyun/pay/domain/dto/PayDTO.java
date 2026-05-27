package com.liyun.pay.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderId;  // 订单号

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;  // 支付渠道，如 ALIPAY, WECHAT

    @NotNull(message = "支付金额不能为空")
    private Long amount;        // 支付金额，分为单位

    @NotNull(message = "收货地址不能为空")
    private Long addressId;     // 收货地址 ID

    private String note;        // 订单备注

    private Long couponId;      // 优惠券 ID

}
