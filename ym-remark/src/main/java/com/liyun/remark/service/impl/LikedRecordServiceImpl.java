package com.liyun.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.context.UserContext;
import com.liyun.remark.constants.RedisConstants;
import com.liyun.remark.domain.dto.LikeRecordFormDTO;
import com.liyun.remark.domain.dto.LikedTimesDTO;
import com.liyun.remark.domain.dto.RemarkEventDTO;
import com.liyun.remark.domain.po.LikedRecord;
import com.liyun.remark.exception.BizIllegalException;
import com.liyun.remark.mapper.LikedRecordMapper;
import com.liyun.remark.mq.RemarkMqSender;
import com.liyun.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final StringRedisTemplate redisTemplate;
    private final RemarkMqSender remarkMqSender;

    @Override
    public void addLikeRecord(LikeRecordFormDTO recordFormDTO) {
        Long userId = UserContext.getUserId();
        Long bizId = recordFormDTO.getBizId();
        String bizType = recordFormDTO.getBizType();
        boolean liked = recordFormDTO.getLiked();

        // 1.Redis Set 点赞/取消点赞
        String userKey = RedisConstants.LIKED_USERS_KEY_PREFIX + bizType + ":" + bizId;
        if (liked) {
            Long added = redisTemplate.opsForSet().add(userKey, userId.toString());
            if (added == null || added == 0) {
                throw new BizIllegalException("已经点过赞了");
            }
        } else {
            Long removed = redisTemplate.opsForSet().remove(userKey, userId.toString());
            if (removed == null || removed == 0) {
                throw new BizIllegalException("尚未点赞");
            }
        }

        // 2.更新 Redis ZSet 点赞数缓存
        String timesKey = RedisConstants.LIKED_TIMES_KEY_PREFIX + bizType;
        redisTemplate.opsForZSet().incrementScore(timesKey, bizId.toString(), liked ? 1 : -1);

        // 3.发送MQ通知
        RemarkEventDTO event = new RemarkEventDTO();
        event.setBizId(bizId);
        event.setBizType(bizType);
        event.setUserId(userId);
        event.setEventType(liked ? "LIKE" : "UNLIKE");
        remarkMqSender.sendRemarkEvent(event);
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long userId = UserContext.getUserId();
        List<LikedRecord> list = lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .in(LikedRecord::getBizId, bizIds)
                .list();
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        String timesKey = RedisConstants.LIKED_TIMES_KEY_PREFIX + bizType;
        // 使用 ZPOPMIN 弹出最小的N个数据并同步
        for (int i = 0; i < maxBizSize; i++) {
            Set<String> popped = redisTemplate.opsForZSet().rangeByScore(timesKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0);
            if (popped == null || popped.isEmpty()) {
                break;
            }
            String bizIdStr = popped.iterator().next();
            Double score = redisTemplate.opsForZSet().score(timesKey, bizIdStr);
            if (score == null) break;

            // 发送MQ同步点赞数据
            LikedTimesDTO dto = new LikedTimesDTO(Long.valueOf(bizIdStr), score.intValue());
            remarkMqSender.sendLikedTimes(bizType, dto);

            // 移除已处理的数据
            redisTemplate.opsForZSet().remove(timesKey, bizIdStr);
        }
    }
}
