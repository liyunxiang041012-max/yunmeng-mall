package com.liyun.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.user.domain.dto.NoticeSaveDTO;
import com.liyun.user.domain.pojo.SystemNotice;
import com.liyun.user.domain.pojo.User;
import com.liyun.user.domain.pojo.UserNotice;
import com.liyun.user.mapper.SystemNoticeMapper;
import com.liyun.user.mapper.UserMapper;
import com.liyun.user.mapper.UserNoticeMapper;
import com.liyun.user.service.ISystemNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemNoticeServiceImpl implements ISystemNoticeService {

    private final SystemNoticeMapper systemNoticeMapper;
    private final UserNoticeMapper userNoticeMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void sendNotice(NoticeSaveDTO dto, Long adminId) {
        SystemNotice notice = new SystemNotice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setAdminId(adminId);
        notice.setTargetRole(dto.getTargetRole() != null ? dto.getTargetRole() : -1);
        notice.setDeleted(0);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        systemNoticeMapper.insert(notice);
    }

    @Override
    public Page<Map<String, Object>> pageUserNotices(Long userId, Integer page, Integer size) {
        // 查用户角色
        Integer userRole = getUserRole(userId);

        // 查全体(-1) + 本角色通知，用 selectList 避免分页插件依赖
        LambdaQueryWrapper<SystemNotice> wrapper = new LambdaQueryWrapper<SystemNotice>()
                .eq(SystemNotice::getDeleted, 0)
                .and(w -> w.eq(SystemNotice::getTargetRole, -1).or().eq(SystemNotice::getTargetRole, userRole))
                .orderByDesc(SystemNotice::getCreateTime);

        List<SystemNotice> allList = systemNoticeMapper.selectList(wrapper);
        long total = allList.size();

        // 手动分页
        int from = (page - 1) * size;
        int to = Math.min(from + size, (int) total);
        List<SystemNotice> pageList = (from < total) ? allList.subList(from, to) : Collections.emptyList();

        if (pageList.isEmpty()) {
            Page<Map<String, Object>> empty = new Page<>(page, size);
            empty.setRecords(Collections.emptyList());
            empty.setTotal(total);
            return empty;
        }

        // 查已读记录
        List<Long> noticeIds = pageList.stream().map(SystemNotice::getId).collect(Collectors.toList());
        Set<Long> readIds = userNoticeMapper.selectList(
                new LambdaQueryWrapper<UserNotice>()
                        .eq(UserNotice::getUserId, userId)
                        .in(UserNotice::getNoticeId, noticeIds))
                .stream().map(UserNotice::getNoticeId).collect(Collectors.toSet());

        List<Map<String, Object>> list = pageList.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("createTime", n.getCreateTime());
            m.put("isRead", readIds.contains(n.getId()));
            return m;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> res = new Page<>(page, size);
        res.setRecords(list);
        res.setTotal(total);
        return res;
    }

    @Override
    public void markRead(Long userId, Long noticeId) {
        boolean exists = userNoticeMapper.exists(
                new LambdaQueryWrapper<UserNotice>()
                        .eq(UserNotice::getUserId, userId)
                        .eq(UserNotice::getNoticeId, noticeId));
        if (!exists) {
            UserNotice un = new UserNotice();
            un.setUserId(userId);
            un.setNoticeId(noticeId);
            un.setReadTime(LocalDateTime.now());
            userNoticeMapper.insert(un);
        }
    }

    @Override
    public long unreadCount(Long userId) {
        Integer userRole = getUserRole(userId);
        long total = systemNoticeMapper.selectCount(
                new LambdaQueryWrapper<SystemNotice>()
                        .eq(SystemNotice::getDeleted, 0)
                        .and(w -> w.eq(SystemNotice::getTargetRole, -1).or().eq(SystemNotice::getTargetRole, userRole)));
        long read = userNoticeMapper.selectCount(
                new LambdaQueryWrapper<UserNotice>()
                        .eq(UserNotice::getUserId, userId));
        return Math.max(0, total - read);
    }

    /** 从 user 表查角色 */
    private Integer getUserRole(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getRole() : 0;
    }
}
