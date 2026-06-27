package com.liyun.pay.handler;

import com.liyun.pay.service.IOrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderJobHandler {

    private final IOrderService orderService;

    /** 扫描超时未支付订单并自动删除 */
    @XxlJob("orderTimeoutCancel")
    public void handleTimeoutOrders() {
        log.info("========================================");
        log.info("【订单清除】XXL-JOB 触发，开始扫描超时订单...");
        long start = System.currentTimeMillis();
        try {
            orderService.handleTimeoutOrders();
            log.info("【订单清除】扫描完成，耗时: {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("【订单清除】扫描失败", e);
        }
        log.info("========================================");
    }
}
