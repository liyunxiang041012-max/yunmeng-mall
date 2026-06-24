package com.liyun.pay.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建订单返回结果
 */
@Data
@AllArgsConstructor
public class CreateOrderVO {
    /** 订单号 */
    private String orderId;
    /** 过期时间（前端用于倒计时） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
