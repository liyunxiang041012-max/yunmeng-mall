package com.liyun.api.client;

import com.liyun.api.dto.RegisterShopDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "ym-user", contextId = "userFeign")
public interface UserFeign {

    @PostMapping("/user/shop/register")
    Long registerShop(@RequestBody RegisterShopDTO registerDTO);
}
