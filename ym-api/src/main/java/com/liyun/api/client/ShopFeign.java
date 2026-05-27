package com.liyun.api.client;

import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.api.vo.ShopCartVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ym-item", contextId = "shopFeign")
public interface ShopFeign {

    @GetMapping("/shop/cart/{id}")
    ShopCartVO getShop(@PathVariable("id") Long id);

    @PostMapping("/shop/batch-info")
    List<ShopCartVO> batchGetShop(@RequestBody List<Long> shopIds);

    @GetMapping("/shop/info/{shopId}")
    ShopInfoDTO getShopInfo(@PathVariable("shopId") Long shopId);

    @PostMapping("/shop/batch-detail")
    List<ShopInfoDTO> batchGetShopInfo(@RequestBody List<Long> shopIds);
}
