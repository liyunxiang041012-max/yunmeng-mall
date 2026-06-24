package com.liyun.remark.task;

import com.liyun.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 已由 LikedTimesSyncJobHandler (XXL-Job) 替代
 * 如需回退到 @Scheduled，取消下方注释并删除 LikedTimesSyncJobHandler
 */
// @Component
@RequiredArgsConstructor
public class LikedTimesSyncTask {

    private static final int MAX_BIZ_SIZE = 30;
    private static final List<String> BIZ_TYPES = List.of("comment", "product");

    private final ILikedRecordService recordService;

    @Scheduled(fixedDelay = 20000)
    public void checkLikedTimes() {
        for (String bizType : BIZ_TYPES) {
            recordService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
        }
    }
}
