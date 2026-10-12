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
 * 商品分类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类id，顶级为0
     */
    private Long parentId;

    /**
     * 层级 1/2/3
     */
    private Integer level;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 1正常 0禁用
     */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
