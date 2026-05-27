package com.liyun.user.service.impl;

import com.liyun.common.constants.OssDirConstants;
import com.liyun.common.context.UserContext;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.exception.BizException;
import com.liyun.common.utils.DateUtils;
import com.liyun.common.utils.OssUtils;
import com.liyun.user.domain.pojo.UserProfile;
import com.liyun.user.mapper.UserProfileMapper;
import com.liyun.user.service.IUserProfileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户个人资料表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements IUserProfileService {

    private final OssUtils ossUtils;

    @Override
    public String uploadAvatar(MultipartFile file) {
        // 1. 获取当前登录用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 2. 查询用户Profile
        UserProfile userProfile = lambdaQuery()
                .eq(UserProfile::getId, userId)
                .one();

        if (userProfile == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }

        // 3. 上传到OSS
        String avatarUrl = ossUtils.upload(file, OssDirConstants.USER_HEADER);

        // 4. 更新头像
        userProfile.setAvatar(avatarUrl);
        userProfile.setUpdateTime(DateUtils.now());
        updateById(userProfile);

        log.info("用户头像更新成功，用户ID: {}, 头像URL: {}", userId, avatarUrl);
        return avatarUrl;
    }
}
