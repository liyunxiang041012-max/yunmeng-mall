package com.liyun.pay.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * ym-pay 内部直连 ym-item 的 Feign 客户端（不依赖 ym-api）
 * 用于扣减库存、增加销量
 */
@FeignClient(name = "ym-item", contextId = "itemInternalFeign")
public interface ItemInternalFeign {

    /** 批量扣减库存 {skuId: qty} */
    @PostMapping("/sku/deduct-stock")
    Map<String, Object> deductStock(@RequestBody Map<Long, Integer> skuQtyMap);

    /** 批量增加销量 {itemId: qty} */
    @PostMapping("/sku/add-sold")
    Map<String, Object> addSold(@RequestBody Map<Long, Integer> itemQtyMap);
}
