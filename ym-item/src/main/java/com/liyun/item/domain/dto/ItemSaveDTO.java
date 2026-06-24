package com.liyun.item.domain.dto;

import lombok.Data;

/**
 * 商家保存/编辑商品请求
 */
@Data
public class ItemSaveDTO {
    private String name;
    private String image;
    private Long categoryId;
    private Long brandId;
    private Long price;
    private Integer stock;
}
