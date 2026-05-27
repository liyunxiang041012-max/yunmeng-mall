package com.liyun.user.service;

import com.liyun.user.domain.dto.LoginDTO;
import com.liyun.user.domain.dto.RegisterDTO;
import com.liyun.user.domain.dto.RegisterShopDTO;
import com.liyun.user.domain.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.user.domain.vo.LoginVO;
import com.liyun.user.domain.vo.UserDetailVO;

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

    String sendSms(String phone);

    void register(RegisterDTO registerDTO, String ip);

    UserDetailVO getUserDetail();

    void logout();

    Long registerShop(RegisterShopDTO registerDTO, String ip);
}
