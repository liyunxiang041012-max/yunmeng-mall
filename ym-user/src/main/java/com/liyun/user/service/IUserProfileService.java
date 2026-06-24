package com.liyun.user.service;

import com.liyun.user.domain.pojo.UserProfile;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserProfileService extends IService<UserProfile> {

    /** 更新用户头像 */
    String updateAvatar(Long userId, String avatarUrl);
}
