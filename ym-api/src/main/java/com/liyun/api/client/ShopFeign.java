package com.liyun.api.client;

import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.api.vo.ShopCartVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 注意：文件上传不适合通过 Feign 调用，建议前端直接调用 ym-item 服务
     * 此接口保留供内部服务调用参考
     */
    @PostMapping(value = "/shop/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadShopAvatar(@RequestPart("file") MultipartFile file);
}
