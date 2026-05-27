package com.liyun.user.service.impl;

import com.liyun.user.domain.pojo.UserProfile;
import com.liyun.user.mapper.UserProfileMapper;
import com.liyun.user.service.IUserProfileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户个人资料表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-13
 */
@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements IUserProfileService {

}
