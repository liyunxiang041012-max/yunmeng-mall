package com.liyun.promotion.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 用户id */
    private Long userId;
    /** 优惠券id */
    private Long couponId;
    /** 用户券id */
    private Long userCouponId;
    /** 事件类型：ISSUE=发放, USE=使用, EXPIRE=过期 */
    private String eventType;
}
