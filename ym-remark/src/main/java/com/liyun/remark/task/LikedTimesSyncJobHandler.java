package com.liyun.remark.task;

import com.liyun.remark.service.ILikedRecordService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 点赞数据同步定时任务（XXL-Job版本）
 * 替代原来的 @Scheduled 实现，支持分片和统一调度管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LikedTimesSyncJobHandler {

    private static final int MAX_BIZ_SIZE = 30;
    private static final List<String> BIZ_TYPES = List.of("comment", "product");

    private final ILikedRecordService recordService;

    @XxlJob("likedTimesSync")
    public void checkLikedTimes() {
        log.info("开始同步点赞数据");
        try {
            for (String bizType : BIZ_TYPES) {
                recordService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
            }
            log.info("点赞数据同步完成");
        } catch (Exception e) {
            log.error("点赞数据同步失败", e);
        }
    }
}
