package com.liyun.user.controller;


import com.liyun.common.utils.Result;
import com.liyun.user.service.IUserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户个人资料表 前端控制器
 * </p>
 *
 * @author liyun
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/user-profile")
@Tag(name = "用户资料", description = "用户资料管理")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserProfileService userProfileService;

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userProfileService.uploadAvatar(file));
    }
}
