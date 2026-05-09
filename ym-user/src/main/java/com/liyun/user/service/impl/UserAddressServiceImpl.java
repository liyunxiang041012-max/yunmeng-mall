package com.liyun.user.service.impl;

import com.liyun.user.domain.pojo.UserAddress;
import com.liyun.user.mapper.UserAddressMapper;
import com.liyun.user.service.IUserAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 收货地址表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-07
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements IUserAddressService {

}
