package com.liyun.item.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商家
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("shop")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户id
     */
    private Long userId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺logo
     */
    private String logo;

    /**
     * 店铺描述
     */
    private String description;

    /**
     * 1营业 0关闭
     */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
