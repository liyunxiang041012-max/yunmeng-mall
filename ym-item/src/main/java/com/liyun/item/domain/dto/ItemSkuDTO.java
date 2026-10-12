package com.liyun.item.domain.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ItemSkuDTO {

    private Long id;

    private String skuName;

    private Long price;

    private Integer stock;

    private String image;

    private Map<String, String> specData;
}