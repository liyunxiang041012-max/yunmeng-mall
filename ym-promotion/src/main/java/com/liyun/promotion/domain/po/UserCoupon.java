package com.liyun.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.liyun.promotion.enums.UserCouponStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long couponId;

    private LocalDateTime termBeginTime;

    private LocalDateTime termEndTime;

    private LocalDateTime usedTime;

    private UserCouponStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
