package com.liyun.user.service;

import com.liyun.user.domain.dto.AddressDTO;
import com.liyun.user.domain.pojo.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IUserAddressService extends IService<UserAddress> {

    void add(AddressDTO addressDTO);

    void updateAddress(Long id, AddressDTO addressDTO);

    void setDefault(Long id);

    /** 查用户地址列表 */
    List<UserAddress> listByUserId(Long userId);

    /** 删地址 */
    void deleteAddress(Long id);
}
