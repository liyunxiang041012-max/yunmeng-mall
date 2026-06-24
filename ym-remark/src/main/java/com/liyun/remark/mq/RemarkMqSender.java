package com.liyun.remark.mq;

import com.liyun.remark.constants.MqConstants;
import com.liyun.remark.domain.dto.LikedTimesDTO;
import com.liyun.remark.domain.dto.RemarkEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemarkMqSender {

    private final RabbitTemplate rabbitTemplate;

    /** 发送互动事件（点赞/取消点赞） */
    public void sendRemarkEvent(RemarkEventDTO dto) {
        rabbitTemplate.convertAndSend(MqConstants.REMARK_EXCHANGE, MqConstants.LIKED_TIMES_KEY, dto);
    }

    /** 发送评论事件 */
    public void sendCommentEvent(RemarkEventDTO dto) {
        rabbitTemplate.convertAndSend(MqConstants.REMARK_EXCHANGE, MqConstants.COMMENT_EVENT_KEY, dto);
    }

    /** 发送点赞数据同步 */
    public void sendLikedTimes(String bizType, LikedTimesDTO dto) {
        rabbitTemplate.convertAndSend(MqConstants.REMARK_EXCHANGE, MqConstants.LIKED_TIMES_KEY, dto);
    }
}
