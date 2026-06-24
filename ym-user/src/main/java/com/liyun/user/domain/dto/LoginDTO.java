package com.liyun.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求参数")
public class LoginDTO {

    @Schema(description = "账号（手机号或用户名）", example = "13800138000")
    private String account;

    @Schema(description = "密码", example = "123456")
    private String password;
}