package com.liyun.remark.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long bizId;

    private String bizType;

    private Long parentId;

    private String content;

    private Integer likedTimes;

    private Integer replyCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
