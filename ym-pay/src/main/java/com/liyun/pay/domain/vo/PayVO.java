package com.liyun.pay.domain.vo;

import com.liyun.pay.enums.PayStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayVO {
    private Long id;
    private Long userId;
    private String orderId;
    private String payNo;
    private String payChannel;
    private Long amount;
    private PayStatus status;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private String payUrl;  // 支付链接/二维码数据
}
