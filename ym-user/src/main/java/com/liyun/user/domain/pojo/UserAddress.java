package com.liyun.user.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 收货地址表
 * </p>
 *
 * @author liyun
 * @since 2026-05-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_address")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using =  ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 收货人
     */
    @TableField("receiver")
    private String receiver;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 完整地址（省市区+详细）
     */
    @TableField("address")
    private String address;

    /**
     * 是否默认 0否 1是
     */
    @TableField("is_default")
    private Integer isDefault;


    @TableField("update_time")
    private LocalDateTime updateTime;


}
