package com.liyun.item.domain.vo;

import lombok.Data;

@Data
public class ItemVO {

    private Long id;

    private String name;

    private String image;

    private Long price;

    private Integer sold;

    private String shopName;

    private Integer status;
}