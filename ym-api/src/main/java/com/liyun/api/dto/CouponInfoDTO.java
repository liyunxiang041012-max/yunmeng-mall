package com.liyun.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户优惠券信息 DTO（用于 Feign 远程调用）
 */
@Data
public class CouponInfoDTO {
    /** 用户券ID */
    private Long id;
    /** 优惠券名称 */
    private String name;
    /** 是否限定使用范围 */
    private Boolean specific;
    /** 优惠券类型：1每满减 2折扣 3无门槛 4普通满减 */
    private Integer discountType;
    /** 折扣门槛，0代表无门槛 */
    private Integer thresholdAmount;
    /** 折扣值 */
    private Integer discountValue;
    /** 最大优惠金额 */
    private Integer maxDiscountAmount;
    /** 有效天数 */
    private Integer termDays;
    /** 使用有效期结束时间 */
    private LocalDateTime termEndTime;
}
