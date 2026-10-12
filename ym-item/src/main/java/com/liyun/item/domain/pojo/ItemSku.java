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
 * SKU
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("item_sku")
public class ItemSku implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联SPU id
     */
    private Long itemId;

    /**
     * SKU名称 如红色-M
     */
    private String skuName;

    /**
     * 价格（分）
     */
    private Long price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * SKU图片
     */
    private String image;

    /**
     * 规格组合 如{"颜色":"红","尺码":"M"}
     */
    private String specData;

    /**
     * 1正常 0禁用
     */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
