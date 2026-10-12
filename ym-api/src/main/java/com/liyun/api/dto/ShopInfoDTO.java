package com.liyun.api.dto;

import lombok.Data;

/**
 * 店铺详细信息 DTO
 */
@Data
public class ShopInfoDTO {
    private Long id;
    private Long userId;
    private String shopName;
    private String logo;
    private String description;
    private Integer status;
}
