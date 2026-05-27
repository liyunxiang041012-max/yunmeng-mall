package com.liyun.pay.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CartVO {
    private Long id;
    private Long userId;
    private Long skuId;
    private Long shopId;        // 商家ID（用于调 ShopFeign）
    private String shopName;    // 商家名称（前端要显示）
    private String skuName;
    private Long price;      // 单位：分（实时价格）

    private String image;
    private Integer quantity;
    private Boolean selected;   // 是否选中
    private Boolean priceChanged; // 价格是否变动
    private LocalDateTime createTime;
    private Long snapshotPrice;  // 改成Long

    private Boolean offShelf;    // 新增，商品是否下架
}
