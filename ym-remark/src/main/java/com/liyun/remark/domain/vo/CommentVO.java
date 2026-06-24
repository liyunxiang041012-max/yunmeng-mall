package com.liyun.remark.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "评论返回实体")
public class CommentVO {
    @Schema(description = "评论id")
    private Long id;

    @Schema(description = "评论用户id")
    private Long userId;

    @Schema(description = "评论目标业务id")
    private Long bizId;

    @Schema(description = "父评论id")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞次数")
    private Integer likedTimes;

    @Schema(description = "回复数量")
    private Integer replyCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
