package com.liyun.item.job;

import com.liyun.item.service.IItemService;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemSyncJobHandler {

    private final IItemService itemService;

    @XxlJob("itemSyncToEsJob")
    public void syncItemToEs() {
        log.info("开始全量同步商品数据到ES");
        long start = System.currentTimeMillis();
        try {
            itemService.syncToEs();
            long cost = System.currentTimeMillis() - start;
            log.info("ES同步完成，耗时{}ms", cost);
            XxlJobHelper.handleSuccess("同步完成，耗时" + cost + "ms");
        } catch (Exception e) {
            log.error("ES同步失败", e);
            XxlJobHelper.handleFail("同步失败：" + e.getMessage());
        }
    }
}