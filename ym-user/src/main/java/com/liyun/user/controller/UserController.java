package com.liyun.user.controller;



import com.liyun.common.context.UserContext;
import com.liyun.common.utils.Result;
import com.liyun.user.domain.dto.LoginDTO;
import com.liyun.user.domain.dto.RegisterDTO;
import com.liyun.user.domain.dto.RegisterShopDTO;
import com.liyun.user.domain.dto.UpdateProfileDTO;
import com.liyun.user.domain.pojo.User;
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

    @Operation(summary = "商家登录")
    @PostMapping("/shop/login")
    public Result<LoginVO> shopLogin(@RequestBody LoginDTO loginDTO) {
        LoginVO vo = userService.shopLogin(loginDTO);
        return Result.success(vo);
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/admin/login")
    public Result<LoginVO> adminLogin(@RequestBody LoginDTO loginDTO) {
        LoginVO vo = userService.adminLogin(loginDTO);
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
    public Result<Long> registerShop(@RequestBody RegisterShopDTO registerDTO, HttpServletRequest  request){
        String ip = getClientIp( request);
        return Result.success(userService.registerShop(registerDTO, ip));
    }
    @Operation(summary = "获取用户详细信息")
    @GetMapping("/detail")
    public Result<UserDetailVO> info() {
         UserDetailVO vo = userService.getUserDetail();

        return Result.success(vo);
    }
    @Operation(summary = "获取当前用户信息（无参，前端兼容）")
    @GetMapping("/info")
    public Result<UserDetailVO> getCurrentUserInfo() {
        UserDetailVO vo = userService.getUserDetail();
        return Result.success(vo);
    }

    @Operation(summary = "按ID获取用户信息")
    @GetMapping("/info/{id}")
    public Result<Map<String, Object>> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result logout() {
        userService.logout();
        return Result.success();
    }

    @Operation(summary = "完善/更新个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        userService.updateProfile(userId, dto);
        return Result.success();
    }

    @Operation(summary = "判断当前用户是否已完成初次设置")
    @GetMapping("/profile/status")
    public Result<Boolean> profileStatus() {
        boolean completed = userService.isProfileCompleted();
        return Result.success(completed);
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
