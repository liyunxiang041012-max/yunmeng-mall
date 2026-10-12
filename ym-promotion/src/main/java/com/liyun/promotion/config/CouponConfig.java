package com.liyun.promotion.config;

import com.liyun.promotion.mq.CouponMqSender;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 优惠券 MQ 配置：声明交换机、队列及绑定关系
 */
@Configuration
public class CouponConfig {

    @Bean
    public TopicExchange couponExchange() {
        return ExchangeBuilder.topicExchange(CouponMqSender.COUPON_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue couponIssueQueue() {
        return QueueBuilder.durable("coupon.issue.queue").build();
    }

    @Bean
    public Queue couponUseQueue() {
        return QueueBuilder.durable("coupon.use.queue").build();
    }

    @Bean
    public Queue couponExpireQueue() {
        return QueueBuilder.durable("coupon.expire.queue").build();
    }

    @Bean
    public Binding couponIssueBinding() {
        return BindingBuilder.bind(couponIssueQueue()).to(couponExchange()).with(CouponMqSender.COUPON_ISSUE_KEY);
    }

    @Bean
    public Binding couponUseBinding() {
        return BindingBuilder.bind(couponUseQueue()).to(couponExchange()).with(CouponMqSender.COUPON_USE_KEY);
    }

    @Bean
    public Binding couponExpireBinding() {
        return BindingBuilder.bind(couponExpireQueue()).to(couponExchange()).with(CouponMqSender.COUPON_EXPIRE_KEY);
    }
}
