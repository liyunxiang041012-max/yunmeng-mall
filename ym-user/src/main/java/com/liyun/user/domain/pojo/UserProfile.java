package com.liyun.user.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户个人资料表
 * </p>
 *
 * @author liyun
 * @since 2026-05-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_profile")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 与user表id一致，1对1
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像url
     */
    private String avatar;

    /**
     * 0未知 1男 2女
     */
    private Integer gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 所在省份
     */
    private String province;



    private LocalDateTime updateTime;

    private String ip;
    private String region;

    /**
     * 会员经验 1-5
     */
    @TableField("experience")
    private Integer experience;



}
