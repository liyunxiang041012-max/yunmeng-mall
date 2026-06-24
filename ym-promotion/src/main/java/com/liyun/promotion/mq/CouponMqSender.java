package com.liyun.promotion.mq;

import com.liyun.promotion.domain.dto.CouponEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponMqSender {

    private final RabbitTemplate rabbitTemplate;

    public static final String COUPON_EXCHANGE = "coupon.exchange";
    public static final String COUPON_ISSUE_KEY = "coupon.issue";
    public static final String COUPON_USE_KEY = "coupon.use";
    public static final String COUPON_EXPIRE_KEY = "coupon.expire";

    /** 发送优惠券发放事件 */
    public void sendCouponIssueEvent(CouponEventDTO dto) {
        dto.setEventType("ISSUE");
        rabbitTemplate.convertAndSend(COUPON_EXCHANGE, COUPON_ISSUE_KEY, dto);
    }

    /** 发送优惠券使用事件 */
    public void sendCouponUseEvent(CouponEventDTO dto) {
        dto.setEventType("USE");
        rabbitTemplate.convertAndSend(COUPON_EXCHANGE, COUPON_USE_KEY, dto);
    }

    /** 发送优惠券过期事件 */
    public void sendCouponExpireEvent(CouponEventDTO dto) {
        dto.setEventType("EXPIRE");
        rabbitTemplate.convertAndSend(COUPON_EXCHANGE, COUPON_EXPIRE_KEY, dto);
    }
}
