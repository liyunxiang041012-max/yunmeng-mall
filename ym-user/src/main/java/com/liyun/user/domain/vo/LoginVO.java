package com.liyun.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// domain/vo/LoginVO.java
@Data
@Schema(description = "登录返回结果")
public class LoginVO {
    @Schema(description = "token")
    private String token;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "角色")
    private Integer role;
}