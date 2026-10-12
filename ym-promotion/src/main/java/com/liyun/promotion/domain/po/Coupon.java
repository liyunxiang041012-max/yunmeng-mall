package com.liyun.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.liyun.promotion.enums.CouponStatus;
import com.liyun.promotion.enums.DiscountType;
import com.liyun.promotion.enums.ObtainType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("coupon")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("`name`")
    private String name;

    private Integer type;

    private DiscountType discountType;

    @TableField("`specific`")
    private Boolean specific;

    private Integer discountValue;

    private Integer thresholdAmount;

    private Integer maxDiscountAmount;

    private ObtainType obtainWay;

    private LocalDateTime issueBeginTime;

    private LocalDateTime issueEndTime;

    private Integer termDays;

    private LocalDateTime termBeginTime;

    private LocalDateTime termEndTime;

    private CouponStatus status;

    private Integer totalNum;

    private Integer issueNum;

    private Integer usedNum;

    private Integer userLimit;

    private String extParam;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long creater;

    private Long updater;

}
