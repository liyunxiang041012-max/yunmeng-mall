package com.liyun.pay.domain.vo;

import lombok.Data;

/**
 * 订单明细 VO
 */
@Data
public class OrderItemVO {

    /**
     * 主键
     */
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

    /**
     * 小计金额（分）
     */
    private Long subtotal;
}
