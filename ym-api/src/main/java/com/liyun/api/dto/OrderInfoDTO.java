package com.liyun.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单信息 DTO（用于 Feign 远程调用）
 */
@Data
public class OrderInfoDTO {
    /** 订单号 */
    private String id;
    /** 用户ID */
    private Long userId;
    /** 店铺ID */
    private Long shopId;
    /** 订单总金额（分） */
    private Long totalAmount;
    /** 实付金额（分） */
    private Long payAmount;
    /** 使用的优惠券ID */
    private Long couponId;
    /** 优惠金额（分） */
    private Long discountAmount;
    /** 订单状态：0待付款 1已付款 2已发货 3已完成 4已取消 5已退款 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 付款时间 */
    private LocalDateTime payTime;
}
