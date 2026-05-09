package com.liyun.user.controller;


import com.liyun.common.utils.Result;
import com.liyun.user.domain.dto.LoginDTO;
import com.liyun.user.domain.dto.RegisterDTO;
import com.liyun.user.domain.vo.LoginVO;
import com.liyun.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author liyun
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户登录、注册等接口")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> userLogin(@RequestBody LoginDTO loginDTO) {
        LoginVO vo = userService.login(loginDTO);
        return Result.success(vo);
    }

    @Operation(summary = "短信验证")
    @PostMapping("/sendCode")
    public Result sendSMS(@RequestBody Map<String, String> body){
        String phone = body.get("phone");
        String code =  userService.sendSms(phone);
        return Result.success();
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO){
     userService.register(registerDTO);
     return Result.success();
    }

}
