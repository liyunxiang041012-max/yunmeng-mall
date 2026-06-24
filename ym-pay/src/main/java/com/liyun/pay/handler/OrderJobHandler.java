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

    /** 扫描超时未支付订单并自动取消 */
    @XxlJob("orderTimeoutCancel")
    public void handleTimeoutOrders() {
        log.info("开始扫描超时未支付订单");
        try {
            orderService.handleTimeoutOrders();
            log.info("超时订单扫描完成");
        } catch (Exception e) {
            log.error("超时订单扫描失败", e);
        }
    }
}
