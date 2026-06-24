package com.liyun.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车信息 DTO（用于 Feign 远程调用）
 */
@Data
public class CartInfoDTO {
    /** 购物车记录ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** SKU ID */
    private Long skuId;
    /** 店铺ID */
    private Long shopId;
    /** 店铺名称 */
    private String shopName;
    /** SKU 名称 */
    private String skuName;
    /** 单价（分） */
    private Long price;
    /** 商品图片 */
    private String image;
    /** 数量 */
    private Integer quantity;
    /** 是否选中 */
    private Boolean selected;
    /** 价格是否变动 */
    private Boolean priceChanged;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 快照价格（分） */
    private Long snapshotPrice;
    /** 商品是否下架 */
    private Boolean offShelf;
}
