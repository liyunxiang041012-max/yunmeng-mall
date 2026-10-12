package com.liyun.remark.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.CollUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.context.UserContext;
import com.liyun.remark.domain.dto.CommentFormDTO;
import com.liyun.remark.domain.dto.RemarkEventDTO;
import com.liyun.remark.domain.po.Comment;
import com.liyun.remark.domain.vo.CommentVO;
import com.liyun.remark.exception.BizIllegalException;
import com.liyun.remark.mapper.CommentMapper;
import com.liyun.remark.mq.RemarkMqSender;
import com.liyun.remark.service.ICommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.liyun.remark.constants.RedisConstants.COMMENT_LOCK_KEY_PREFIX;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    private final RedissonClient redissonClient;
    private final RemarkMqSender remarkMqSender;

    @Override
    @Transactional
    public void saveComment(CommentFormDTO dto) {
        Long userId = UserContext.getUserId();

        // Redisson分布式锁：防止重复评论
        String lockKey = COMMENT_LOCK_KEY_PREFIX + userId + ":" + dto.getBizType() + ":" + dto.getBizId();
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            // 保存评论
            Comment comment = BeanUtils.copyBean(dto, Comment.class);
            comment.setUserId(userId);
            comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
            save(comment);

            // 如果是回复，更新父评论的回复数量
            if (dto.getParentId() != null && dto.getParentId() > 0) {
                lambdaUpdate()
                        .setSql("reply_count = reply_count + 1")
                        .eq(Comment::getId, dto.getParentId())
                        .update();
            }

            // 发送MQ通知
            RemarkEventDTO event = new RemarkEventDTO();
            event.setBizId(dto.getBizId());
            event.setBizType(dto.getBizType());
            event.setUserId(userId);
            event.setEventType(dto.getParentId() != null && dto.getParentId() > 0 ? "REPLY" : "COMMENT");
            event.setRefId(comment.getId());
            remarkMqSender.sendCommentEvent(event);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PageDTO<CommentVO> pageQueryComments(Long bizId, String bizType, int pageNo, int pageSize) {
        Page<Comment> page = lambdaQuery()
                .eq(Comment::getBizId, bizId)
                .eq(Comment::getBizType, bizType)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreateTime)
                .page(new Page<>(pageNo, pageSize));

        List<Comment> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        List<CommentVO> voList = BeanUtils.copyList(records, CommentVO.class);
        return PageDTO.of(page, voList);
    }
}
