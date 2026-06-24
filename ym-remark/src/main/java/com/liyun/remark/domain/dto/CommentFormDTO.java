package com.liyun.remark.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "评论表单实体")
public class CommentFormDTO {
    @Schema(description = "评论目标业务id（如商品id）")
    @NotNull(message = "业务id不能为空")
    private Long bizId;

    @Schema(description = "评论业务类型，如：product")
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "父评论id，0表示为一级评论")
    private Long parentId;

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不能为空")
    private String content;
}
