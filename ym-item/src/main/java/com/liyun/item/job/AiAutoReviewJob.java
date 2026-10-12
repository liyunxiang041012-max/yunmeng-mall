package com.liyun.item.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.service.IItemService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAutoReviewJob {

    private final IItemService itemService;
    private final RestTemplate restTemplate;

    private static final String REVIEW_URL = "http://yunmeng-ai/review/item";

    @XxlJob("aiAutoReviewHandler")
    public void autoReview() {
        long start = System.currentTimeMillis();
        log.info("========================================");
        log.info("【AI审核】XXL-JOB 触发，开始执行...");
        log.info("【AI审核】调用地址: {}", REVIEW_URL);

        // 1. 查待审核
        List<Item> pendingList = itemService.list(
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getAuditStatus, 0)
                        .eq(Item::getDeleted, 0));
        log.info("【AI审核】STEP1-查询待审核: 共 {} 件", pendingList.size());

        if (pendingList.isEmpty()) {
            log.info("【AI审核】无待审核商品，任务结束");
            XxlJobHelper.handleSuccess("无待审核商品");
            return;
        }

        // 2. 逐条调用 AI
        int passed = 0, risk = 0, error = 0;
        for (int i = 0; i < pendingList.size(); i++) {
            Item item = pendingList.get(i);
            log.info("【AI审核】STEP2-处理 [{}/{}] id={}, name={}, price={}",
                    i + 1, pendingList.size(), item.getId(), item.getName(), item.getPrice());
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("name", item.getName());
                body.put("price", item.getPrice());
                body.put("stock", item.getStock());
                log.info("【AI审核】请求体: {}", body);

                Map<String, Object> resp = restTemplate.postForObject(REVIEW_URL, body, Map.class);
                log.info("【AI审核】AI返回: {}", resp);

                if (resp == null || resp.get("data") == null) {
                    log.warn("【AI审核】id={} 返回为空，跳过", item.getId());
                    error++;
                    continue;
                }

                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                String suggestion = (String) data.get("suggestion");
                String reason = (String) data.get("reason");
                log.info("【AI审核】suggestion={}, reason={}", suggestion, reason);

                if ("approve".equals(suggestion)) {
                    item.setAuditStatus(1);
                    item.setUpdateTime(LocalDateTime.now());
                    itemService.updateById(item);
                    passed++;
                    log.info("【AI审核】>>> 自动通过: id={}", item.getId());
                } else {
                    risk++;
                    log.info("【AI审核】>>> 有风险，留待人工: id={}", item.getId());
                }
            } catch (Exception e) {
                error++;
                log.error("【AI审核】异常 id={}: {}", item.getId(), e.getMessage(), e);
            }
        }

        long cost = System.currentTimeMillis() - start;
        String msg = String.format("完成: 通过%d件, 风险%d件, 异常%d件, 耗时%dms", passed, risk, error, cost);
        log.info("【AI审核】{}", msg);
        log.info("========================================");
        XxlJobHelper.handleSuccess(msg);
    }
}
