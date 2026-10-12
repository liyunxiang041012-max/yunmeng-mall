package com.liyun.pay.domain.pojo;

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
 * 购物车表
 * </p>
 *
 * @author liyun
 * @since 2026-05-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cart")
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品SKU ID（关联 ym_item.item_sku）
     */
    private Long skuId;

    /**
     * 商品SPU ID（冗余，方便查同店商品）
     */
    private Long spuId;

    /**
     * 店铺ID（结算时按店铺分组）
     */
    private Long shopId;

    /**
     * 商品名称（冗余快照）
     */
    private String name;

    /**
     * 商品主图（冗余快照）
     */
    private String image;

    /**
     * 规格信息，如"颜色:红/尺码:XL"（冗余快照）
     */
    private String specInfo;

    /**
     * 加入时单价，分为单位（冗余快照）
     */
    private Long price;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 是否选中 0否 1是
     */
    private Boolean selected;

    /**
     * 加入时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
