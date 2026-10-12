package com.liyun.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发送系统通知请求")
public class NoticeSaveDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不超过100字")
    @Schema(description = "标题", example = "系统维护通知")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "通知详情", example = "今晚22:00-24:00系统维护")
    private String content;

    @Schema(description = "目标角色: -1=全部, 0=普通用户, 1=商家, 2=管理员", example = "-1")
    private Integer targetRole;
}
