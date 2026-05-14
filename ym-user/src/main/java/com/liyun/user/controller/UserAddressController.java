package com.liyun.user.controller;


import com.liyun.common.context.UserContext;
import com.liyun.common.utils.Result;
import com.liyun.user.domain.dto.AddressDTO;
import com.liyun.user.domain.pojo.UserAddress;
import com.liyun.user.service.IUserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/list")
    @Operation(summary = "查询用户地址列表")
    public Result list(){
        Long userId = UserContext.getUserId();
        List<UserAddress> list = userAddressService.lambdaQuery()
                .eq(UserAddress::getUserId, userId).list();


        return Result.success(list);
    }

    @Operation(summary = "删除用户地址")
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id){
        userAddressService.removeById(id);
        return Result.success();
    }
    @Operation(summary = "修改用户地址")
    @PutMapping("/update/{id}")
    public Result update(@PathVariable Long id,@RequestBody AddressDTO addressDTO){
        userAddressService.updateAddress(id, addressDTO);
        return Result.success();
    }
    @Operation(summary = "修改用户地址默认状态")
    @PutMapping("/setDefault/{id}")
    public Result setDefault(@PathVariable Long id){
        userAddressService.setDefault(id);
        return Result.success();
    }
}
