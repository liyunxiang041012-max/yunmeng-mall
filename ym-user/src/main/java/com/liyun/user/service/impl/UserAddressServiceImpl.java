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
        if (list.size() == 0){
          userAddress.setIsDefault(1);
        }

        log.info("用户id:{}",userId);
        log.info("dto:{}",addressDTO);
        userAddress.setUserId(userId)
                .setReceiver(addressDTO.getReceiver())
                .setPhone(addressDTO.getPhone())
                .setAddress(addressDTO.getAddress())
                .setIsDefault(addressDTO.getIsDefault())
                .setCreateTime(DateUtils.now())
                .setCreateTime(DateUtils.now());
        save(userAddress);



    }
}
