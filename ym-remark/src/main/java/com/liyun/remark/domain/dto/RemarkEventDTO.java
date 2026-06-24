package com.liyun.remark.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemarkEventDTO {
    /** 业务id */
    private Long bizId;
    /** 业务类型 */
    private String bizType;
    /** 用户id */
    private Long userId;
    /** 事件类型：LIKE=点赞, UNLIKE=取消点赞, COMMENT=评论, REPLY=回复 */
    private String eventType;
    /** 关联数据，如点赞数或评论id */
    private Long refId;
}
