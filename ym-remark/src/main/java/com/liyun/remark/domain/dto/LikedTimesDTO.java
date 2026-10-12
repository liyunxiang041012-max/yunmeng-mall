package com.liyun.remark.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikedTimesDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 点赞的业务id */
    private Long bizId;
    /** 总的点赞次数 */
    private Integer likedTimes;

    public static LikedTimesDTO of(Long bizId, Integer likedTimes) {
        return new LikedTimesDTO(bizId, likedTimes);
    }
}
