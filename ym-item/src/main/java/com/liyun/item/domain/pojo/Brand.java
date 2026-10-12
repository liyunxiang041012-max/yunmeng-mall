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
 * 品牌
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("brand")
public class Brand implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 品牌名
     */
    private String name;

    /**
     * 品牌logo
     */
    private String logo;

    /**
     * 品牌描述
     */
    private String description;

    /**
     * 1正常 0禁用
     */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
