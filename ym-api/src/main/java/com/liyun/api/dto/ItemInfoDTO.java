package com.liyun.api.dto;

import lombok.Data;

/**
 * 商品 SPU 信息 DTO
 */
@Data
public class ItemInfoDTO {
    private Long id;
    private Long shopId;
    private Long categoryId;
    private Long brandId;
    private String name;
    private String image;
    private Long price;
    private Integer stock;
    private Integer sold;
    private Integer status;
}
