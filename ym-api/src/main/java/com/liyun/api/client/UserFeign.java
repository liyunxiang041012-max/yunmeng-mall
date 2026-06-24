package com.liyun.api.client;

import com.liyun.api.dto.RegisterShopDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(value = "ym-user", contextId = "userFeign")
public interface UserFeign {

    @PostMapping("/user/shop/register")
    Long registerShop(@RequestBody RegisterShopDTO registerDTO);

    /** 按ID获取用户信息 */
    @GetMapping("/user/info/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long userId);
}
