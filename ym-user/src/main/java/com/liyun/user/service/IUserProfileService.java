package com.liyun.user.service;

import com.liyun.user.domain.pojo.UserProfile;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户个人资料表 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-13
 */
public interface IUserProfileService extends IService<UserProfile> {

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 头像URL
     */
    String uploadAvatar(MultipartFile file);
}
