package com.liyun.api.feign;

import com.liyun.api.dto.RegisterShopDTO;
import com.liyun.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "ym-user")
public interface UserFeign {

    @PostMapping("/user/shop/register")
    Result registerShop(@RequestBody RegisterShopDTO registerDTO);
}
