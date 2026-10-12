package com.liyun.promotion.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * 调用 ym-item 获取 SKU 信息（秒杀折扣校验用）
 */
@FeignClient(name = "ym-item", contextId = "promotionItemFeign")
public interface ItemFeign {

    /** 获取SKU信息（含原价） */
    @GetMapping("/sku/info/{skuId}")
    Map<String, Object> getSkuInfo(@PathVariable("skuId") Long skuId);

    /** 获取店铺商品 ID 列表（内部接口） */
    @GetMapping("/items/shop-item-ids/{shopId}")
    List<Long> getShopItemIds(@PathVariable("shopId") Long shopId);
}
