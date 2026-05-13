package com.liyun.user.controller;


import com.liyun.common.utils.Result;
import com.liyun.user.domain.dto.AddressDTO;
import com.liyun.user.service.IUserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 收货地址表 前端控制器
 * </p>
 *
 * @author liyun
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/address")
@Tag(name = "用户地址管理", description = "用户地址管理接口")
@RequiredArgsConstructor
public class UserAddressController {
    private final IUserAddressService userAddressService;
    @Operation(summary = "添加用户地址")
    @PostMapping("/add")
    public Result add(@RequestBody AddressDTO addressDTO){
        userAddressService.add(addressDTO);
        return Result.success();
    }
    @Operation(summary = "查询用户地址")
    @GetMapping("/list")
    public Result list(){
        userAddressService.query();
        return Result.success(userAddressService.list());
    }
}
