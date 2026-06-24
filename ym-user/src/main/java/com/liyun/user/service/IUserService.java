package com.liyun.user.service;

import com.liyun.user.domain.dto.LoginDTO;
import com.liyun.user.domain.dto.RegisterDTO;
import com.liyun.user.domain.dto.RegisterShopDTO;
import com.liyun.user.domain.dto.UpdateProfileDTO;
import com.liyun.user.domain.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.user.domain.vo.LoginVO;
import com.liyun.user.domain.vo.UserDetailVO;

import java.util.Map;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-07
 */
public interface IUserService extends IService<User> {

    LoginVO login(LoginDTO loginDTO);

    LoginVO shopLogin(LoginDTO loginDTO);

    LoginVO adminLogin(LoginDTO loginDTO);

    String sendSms(String phone);

    void register(RegisterDTO registerDTO, String ip);

    UserDetailVO getUserDetail();

    void logout();

    Long registerShop(RegisterShopDTO registerDTO, String ip);

    /**
     * 按ID获取用户基本信息（供Feign调用）
     */
    Map<String, Object> getUserById(Long userId);

    /**
     * 完善/更新个人资料（生日、性别、省份）
     */
    void updateProfile(Long userId, UpdateProfileDTO dto);

    /**
     * 判断当前用户是否已完成初次设置（city、birthday、province均已填写）
     */
    boolean isProfileCompleted();
}
