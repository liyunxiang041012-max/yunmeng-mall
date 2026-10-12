package com.liyun.item.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class ItemDTO {

    private Long id;

    private Long shopId;

    private Long categoryId;

    private Long brandId;

    private String name;

    private String image;

    private Integer status;

    private List<ItemSkuDTO> skus;

    private String description;

    private List<String> detailImgs;
}