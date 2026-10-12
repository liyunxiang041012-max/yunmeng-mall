// com.liyun.cart.feign.ItemFeign
package com.liyun.api.client;


import com.liyun.api.dto.ItemInfoDTO;
import com.liyun.api.dto.SkuInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.List;

@FeignClient(name = "ym-item", contextId = "itemFeign")
public interface ItemFeign {
    @GetMapping("/sku/info/{skuId}")
    Map<String, Object> getSkuInfo(@PathVariable("skuId") Long skuId);

    @PostMapping("/sku/batch-info")
    Map<String, Object> batchGetSkuInfo(@RequestBody List<Long> skuIds);

    @GetMapping("/item/info/{itemId}")
    Map<String, Object> getItemInfo(@PathVariable("itemId") Long itemId);

    @PostMapping("/item/batch-info")
    Map<String, Object> batchGetItemInfo(@RequestBody List<Long> itemIds);

    /** 搜索商品（AI 助手用） */
    @GetMapping("/items/page")
    Map<String, Object> searchItems(@RequestParam("keyword") String keyword);
}
