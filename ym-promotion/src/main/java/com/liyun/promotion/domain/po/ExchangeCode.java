package com.liyun.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.liyun.promotion.enums.ExchangeCodeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exchange_code")
public class ExchangeCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private String code;

    private ExchangeCodeStatus status;

    private Long userId;

    private Integer type;

    private Long exchangeTargetId;

    private LocalDateTime createTime;

    private LocalDateTime expiredTime;

    private LocalDateTime updateTime;

}
