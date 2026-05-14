package com.liyun.user.service.impl;

import com.liyun.common.utils.DateUtils;
import com.liyun.common.context.UserContext;
import com.liyun.user.domain.dto.AddressDTO;
import com.liyun.user.domain.pojo.UserAddress;
import com.liyun.user.mapper.UserAddressMapper;
import com.liyun.user.service.IUserAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.liyun.common.utils.DateUtils.SIGN_DATE_SUFFIX_FORMATTER;

/**
 * <p>
 * 收货地址表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-07
 */
@Slf4j
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements IUserAddressService {

    @Override
    public void add(AddressDTO addressDTO) {
        UserAddress userAddress = new UserAddress();
        Long userId = UserContext.getUserId();
        List<UserAddress> list = lambdaQuery().eq(UserAddress::getUserId, userId).list();

        if (list.size() >= 5){
            throw new RuntimeException("最多添加5个收货地址");
        }
        if (list.size() == 0) {
            userAddress.setIsDefault(1);
        } else {
            userAddress.setIsDefault(addressDTO.getIsDefault());
        }

        if (userAddress.getIsDefault() == 1){
            UserAddress oldDefault = lambdaQuery().eq(UserAddress::getUserId, userId).eq(UserAddress::getIsDefault, 1).one();
            if (oldDefault != null){
                oldDefault.setIsDefault(0);
                updateById(oldDefault);
            }
        }

        log.info("用户id:{}",userId);
        log.info("dto:{}",addressDTO);
        userAddress.setUserId(userId)
                .setReceiver(addressDTO.getReceiver())
                .setPhone(addressDTO.getPhone())
                .setAddress(addressDTO.getAddress())
                .setUpdateTime(DateUtils.now());

        save(userAddress);



    }

    @Override
    public void updateAddress(Long id, AddressDTO addressDTO) {
        //1.获取用户id
        Long userId = UserContext.getUserId();
        UserAddress userAddress = lambdaQuery().eq(UserAddress::getUserId, userId).eq(UserAddress::getId, id).one();
        //2.判断用户地址是否存在
        if (userAddress == null){
            throw new RuntimeException("用户地址不存在");
        }
        //3.更新用户地址
        userAddress.setReceiver(addressDTO.getReceiver())
                .setPhone(addressDTO.getPhone())
                .setAddress(addressDTO.getAddress())
                .setIsDefault(addressDTO.getIsDefault())
                .setUpdateTime(DateUtils.now());
                updateById(userAddress);

    }

    @Override
    public void setDefault(Long id) {
         Long userId = UserContext.getUserId();
         UserAddress userAddress = lambdaQuery().eq(UserAddress::getUserId, userId).eq(UserAddress::getId, id).one();
        if (userAddress == null){
            throw new RuntimeException("用户地址不存在");
        }
        UserAddress oldDefault = lambdaQuery().eq(UserAddress::getUserId, userId).eq(UserAddress::getIsDefault, 1).one();
        if (oldDefault != null){
            oldDefault.setIsDefault(0);
            updateById(oldDefault);
        }
         userAddress.setIsDefault(1);
         updateById(userAddress);
    }


}
