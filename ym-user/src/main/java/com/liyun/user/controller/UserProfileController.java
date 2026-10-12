package com.liyun.user.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.Result;
import com.liyun.user.domain.pojo.UserProfile;
import com.liyun.user.service.IUserProfileService;
import com.liyun.user.service.impl.OssUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user-profile")
@Tag(name = "用户资料管理", description = "用户个人资料接口")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserProfileService userProfileService;
    private final OssUploadService ossUploadService;

    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }

        // 1. 上传到 OSS
        String avatarUrl = ossUploadService.uploadAvatar(file);
        // 2. 更新到数据库
        userProfileService.updateAvatar(userId, avatarUrl);
        return Result.success(avatarUrl);
    }
}
