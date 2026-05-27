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

import java.util.List;

@FeignClient(name = "ym-item", contextId = "itemFeign")
public interface ItemFeign {
    @GetMapping("/sku/info/{skuId}")
    SkuInfoDTO getSkuInfo(@PathVariable("skuId") Long skuId);

    @PostMapping("/sku/batch-info")
    List<SkuInfoDTO> batchGetSkuInfo(@RequestBody List<Long> skuIds);

    @GetMapping("/item/info/{itemId}")
    ItemInfoDTO getItemInfo(@PathVariable("itemId") Long itemId);

    @PostMapping("/item/batch-info")
    List<ItemInfoDTO> batchGetItemInfo(@RequestBody List<Long> itemIds);

    @PostMapping("/sku/deduct-stock")
    void deductStock(@RequestParam("skuId") Long skuId, @RequestParam("quantity") Integer quantity);
}
