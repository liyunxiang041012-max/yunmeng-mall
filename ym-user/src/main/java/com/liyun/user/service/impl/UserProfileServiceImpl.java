package com.liyun.user.service.impl;

import com.liyun.user.domain.pojo.UserProfile;
import com.liyun.user.mapper.UserProfileMapper;
import com.liyun.user.service.IUserProfileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements IUserProfileService {

    @Override
    public String updateAvatar(Long userId, String avatarUrl) {
        UserProfile profile = getById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setId(userId);
            profile.setAvatar(avatarUrl);
            save(profile);
        } else {
            profile.setAvatar(avatarUrl);
            updateById(profile);
        }
        return avatarUrl;
    }
}
