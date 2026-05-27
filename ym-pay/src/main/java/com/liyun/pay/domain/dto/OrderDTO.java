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
     * 优惠券ID，没有则为null
     */
    private Long couponId;



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