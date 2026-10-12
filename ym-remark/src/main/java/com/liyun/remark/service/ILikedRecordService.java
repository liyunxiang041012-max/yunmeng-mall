package com.liyun.remark.service;

import com.liyun.remark.domain.dto.LikeRecordFormDTO;
import com.liyun.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

public interface ILikedRecordService extends IService<LikedRecord> {

    void addLikeRecord(LikeRecordFormDTO recordFormDTO);

    Set<Long> isBizLiked(List<Long> bizIds);

    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);
}
