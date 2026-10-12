package com.liyun.promotion.mq;

import com.liyun.promotion.domain.dto.CouponEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券事件 MQ 消费者
 * 监听优惠券发放、使用、过期事件，进行日志记录和后续业务处理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponMqListener {

    /** 监听优惠券发放事件 */
    @RabbitListener(queues = "coupon.issue.queue")
    public void onCouponIssue(CouponEventDTO dto) {
        log.info("[MQ] 优惠券发放事件: userId={}, couponId={}, userCouponId={}",
                dto.getUserId(), dto.getCouponId(), dto.getUserCouponId());
        // TODO: 可在此处扩展：发送发放通知、记录统计等
    }

    /** 监听优惠券使用事件 */
    @RabbitListener(queues = "coupon.use.queue")
    public void onCouponUse(CouponEventDTO dto) {
        log.info("[MQ] 优惠券使用事件: userId={}, couponId={}, userCouponId={}",
                dto.getUserId(), dto.getCouponId(), dto.getUserCouponId());
        // TODO: 可在此处扩展：更新使用统计、触发营销分析等
    }

    /** 监听优惠券过期事件 */
    @RabbitListener(queues = "coupon.expire.queue")
    public void onCouponExpire(CouponEventDTO dto) {
        log.info("[MQ] 优惠券过期事件: userId={}, couponId={}, userCouponId={}",
                dto.getUserId(), dto.getCouponId(), dto.getUserCouponId());
        // TODO: 可在此处扩展：发送过期提醒、回收库存等
    }
}
