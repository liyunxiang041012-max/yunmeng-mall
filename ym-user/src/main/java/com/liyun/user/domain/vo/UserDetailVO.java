package com.liyun.user.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.liyun.user.domain.dto.AddressDTO;
import com.liyun.user.domain.pojo.UserAddress;
import lombok.Data;

import java.util.List;

@Data
public class UserDetailVO {

    // ─── 用户基本信息 ───────────────────────────────────────
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String nickname;
    private String username;
    private String avatar;
    private String phone;
    private Integer experience;


    // ─── 收货地址列表 ───────────────────────────────────────
    private List<UserAddress> addresses;


}