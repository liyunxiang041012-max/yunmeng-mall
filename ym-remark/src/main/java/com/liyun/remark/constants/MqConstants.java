package com.liyun.remark.constants;

public interface MqConstants {
    /** 互动事件交换机 */
    String REMARK_EXCHANGE = "remark.exchange";
    /** 点赞数据同步队列 */
    String LIKED_TIMES_QUEUE = "liked.times.queue";
    /** 点赞数据同步Key */
    String LIKED_TIMES_KEY = "liked.times";
    /** 评论事件队列 */
    String COMMENT_EVENT_QUEUE = "comment.event.queue";
    /** 评论事件Key */
    String COMMENT_EVENT_KEY = "comment.event";
}
