package com.liyun.remark.constants;

public interface RedisConstants {
    /** 点赞用户Set Key前缀: liked:users:{bizType}:{bizId} */
    String LIKED_USERS_KEY_PREFIX = "liked:users:";
    /** 点赞统计ZSet Key: liked:times:{bizType} */
    String LIKED_TIMES_KEY_PREFIX = "liked:times:";
    /** 用户评论锁Key前缀 */
    String COMMENT_LOCK_KEY_PREFIX = "lock:comment:";
}
