package com.liyun.user.controller;



import com.liyun.common.utils.Result;
import com.liyun.user.domain.dto.LoginDTO;
import com.liyun.user.domain.dto.RegisterDTO;
import com.liyun.user.domain.dto.RegisterShopDTO;
import com.liyun.user.domain.vo.LoginVO;
import com.liyun.user.domain.vo.UserDetailVO;
import com.liyun.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    public Result register(@RequestBody RegisterDTO registerDTO, HttpServletRequest  request){
        String ip = getClientIp( request);
     userService.register(registerDTO, ip);
     return Result.success();
    }
    @Operation(summary = "商家注册")
    @PostMapping("/shop/register")
    public Result registerShop(@RequestBody RegisterShopDTO registerDTO, HttpServletRequest  request){
        String ip = getClientIp( request);
        userService.registerShop(registerDTO, ip);
        return Result.success();
    }
    @Operation(summary = "获取用户详细信息")
    @GetMapping("/detail")
    public Result<UserDetailVO> info() {
         UserDetailVO vo = userService.getUserDetail();

        return Result.success(vo);
    }
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result logout() {
        userService.logout();
        return Result.success();
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能有多个IP，第一个才是真实客户端
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 本地开发兼容
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
