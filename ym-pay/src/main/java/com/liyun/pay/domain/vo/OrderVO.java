package com.liyun.pay.domain.vo;

import com.liyun.pay.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单 VO
 */
@Data
public class OrderVO {

    /**
     * 订单号
     */
    private String id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 订单总金额（分）
     */
    private Long totalAmount;

    /**
     * 实付金额（分）
     */
    private Long payAmount;

    /**
     * 使用的优惠券ID
     */
    private Long couponId;

    /**
     * 优惠金额（分）
     */
    private Long discountAmount;

    /**
     * 订单状态：0待付款 1已付款 2已发货 3已完成 4已取消 5已退款
     */
    private OrderStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 付款时间
     */
    private LocalDateTime payTime;

    /**
     * 订单状态描述
     */
    private String statusDesc;
}
