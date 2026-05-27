package com.liyun.pay.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.liyun.pay.enums.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author liyun
 * @since 2026-05-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("`order`")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @TableId(value = "id")
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
     * 使用的优惠券ID，未使用则为null
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

}
