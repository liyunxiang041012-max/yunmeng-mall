package com.liyun.item.domain.vo;

import lombok.Data;

/**
 * 店铺详情 VO
 */
@Data
public class ShopDetailVO {

    /**
     * 店铺ID
     */
    private Long id;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺Logo URL
     */
    private String logo;

    /**
     * 店铺描述
     */
    private String description;

    /**
     * 综合评分，如 "4.9"
     */
    private String score;

    /**
     * 好评率，如 "98.6%"
     */
    private String goodRate;

    /**
     * 平均发货时间，如 "24h"
     */
    private String avgShipTime;

    /**
     * 总销量（整数）
     */
    private Long totalSales;

    /**
     * 在售商品总数
     */
    private Integer itemCount;

    /**
     * 粉丝数
     */
    private Long fansCount;
}
