package com.liyun.pay.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单明细表
 * </p>
 *
 * @author liyun
 * @since 2026-05-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * SPU ID
     */
    private Long spuId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 单价（分）
     */
    private Long price;

    /**
     * 数量
     */
    private Integer quantity;

}
