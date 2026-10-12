package com.liyun.remark.config;

import com.liyun.remark.constants.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemarkConfig {

    @Bean
    public TopicExchange remarkExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.REMARK_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue likedTimesQueue() {
        return QueueBuilder.durable(MqConstants.LIKED_TIMES_QUEUE).build();
    }

    @Bean
    public Queue commentEventQueue() {
        return QueueBuilder.durable(MqConstants.COMMENT_EVENT_QUEUE).build();
    }

    @Bean
    public Binding likedTimesBinding() {
        return BindingBuilder.bind(likedTimesQueue()).to(remarkExchange()).with(MqConstants.LIKED_TIMES_KEY);
    }

    @Bean
    public Binding commentEventBinding() {
        return BindingBuilder.bind(commentEventQueue()).to(remarkExchange()).with(MqConstants.COMMENT_EVENT_KEY);
    }
}
