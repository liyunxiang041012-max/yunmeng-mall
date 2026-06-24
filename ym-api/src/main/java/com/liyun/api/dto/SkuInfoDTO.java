package com.liyun.api.dto;

import lombok.Data;

// com.liyun.cart.feign.dto.SkuInfoDTO
@Data
public class SkuInfoDTO {
    private Long id;
    private Long itemId;
    private Long shopId;
    private String name;
    private Integer price;
    private String image;
    private Integer stock;
}

