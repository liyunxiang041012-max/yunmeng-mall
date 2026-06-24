package com.liyun.item.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增/编辑请求
 */
@Data
@Schema(description = "分类保存请求")
public class CategorySaveDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 2, max = 50, message = "分类名称长度需在2-50字符之间")
    @Schema(description = "分类名称", example = "服装")
    private String name;

    @Schema(description = "父分类ID，0=一级分类", example = "0")
    private Long parentId;
}
