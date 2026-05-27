package com.liyun.pay.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.liyun.pay.enums.PayStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 支付表
 * </p>
 *
 * @author liyun
 * @since 2026-05-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pay")
public class Pay implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 支付单号
     */
    private String payNo;

    /**
     * 支付渠道（如 ALIPAY, WECHAT）
     */
    private String payChannel;

    /**
     * 支付金额，分为单位
     */
    private Long amount;

    /**
     * 支付状态（0-待支付 1-已支付 2-已退款 3-已关闭）
     */
    private PayStatus status;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 收货地址 ID
     */
    private Long addressId;

    /**
     * 订单备注
     */
    private String note;

    /**
     * 优惠券 ID
     */
    private Long couponId;

}
