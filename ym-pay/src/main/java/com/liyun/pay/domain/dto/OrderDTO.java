package com.liyun.pay.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderDTO {

    /**
     * 购物车商品列表
     */
    private List<OrderItemDTO> items;

    /**
     * 用户优惠券ID（user_coupon表主键），String类型防JS精度丢失
     */
    private String userCouponId;



    @Data
    public static class OrderItemDTO {

        /**
         * 商品SKU ID
         */
        private Long skuId;

        /**
         * 购买数量
         */
        private Integer quantity;
    }
}