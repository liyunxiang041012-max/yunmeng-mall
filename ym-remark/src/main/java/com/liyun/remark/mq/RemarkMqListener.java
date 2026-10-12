package com.liyun.remark.mq;

import com.liyun.remark.constants.MqConstants;
import com.liyun.remark.domain.dto.LikedTimesDTO;
import com.liyun.remark.domain.dto.RemarkEventDTO;
import com.liyun.remark.domain.po.Comment;
import com.liyun.remark.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 评论/点赞 MQ 消费者
 * 监听点赞同步和评论事件队列
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RemarkMqListener {

    private final CommentMapper commentMapper;

    /**
     * 监听点赞数据同步队列
     * 将 Redis 中的点赞数增量同步到数据库
     */
    @RabbitListener(queues = MqConstants.LIKED_TIMES_QUEUE)
    public void onLikedTimes(Object message) {
        if (message instanceof LikedTimesDTO dto) {
            // 点赞同步：更新评论表的点赞数
            log.info("[MQ] 点赞同步: bizId={}, likedTimes={}", dto.getBizId(), dto.getLikedTimes());
            Comment comment = commentMapper.selectById(dto.getBizId());
            if (comment != null) {
                comment.setLikedTimes((comment.getLikedTimes() == null ? 0 : comment.getLikedTimes()) + dto.getLikedTimes());
                commentMapper.updateById(comment);
            }
        } else if (message instanceof RemarkEventDTO dto) {
            // 点赞/取消点赞事件通知
            log.info("[MQ] 互动事件: bizId={}, bizType={}, userId={}, eventType={}",
                    dto.getBizId(), dto.getBizType(), dto.getUserId(), dto.getEventType());
            // TODO: 可在此处扩展：发送点赞通知给作者等
        }
    }

    /**
     * 监听评论事件队列
     * 处理评论/回复事件的通知
     */
    @RabbitListener(queues = MqConstants.COMMENT_EVENT_QUEUE)
    public void onCommentEvent(RemarkEventDTO dto) {
        log.info("[MQ] 评论事件: bizId={}, bizType={}, userId={}, eventType={}, refId={}",
                dto.getBizId(), dto.getBizType(), dto.getUserId(), dto.getEventType(), dto.getRefId());
        // TODO: 可在此处扩展：发送评论通知给商品作者/被回复者等
    }
}
