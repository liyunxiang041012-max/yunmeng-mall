package com.liyun.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.user.domain.dto.NoticeSaveDTO;

import java.util.Map;

public interface ISystemNoticeService {

    /** 管理员发送通知 */
    void sendNotice(NoticeSaveDTO dto, Long adminId);

    /** 用户分页查询通知列表（含已读状态） */
    Page<Map<String, Object>> pageUserNotices(Long userId, Integer page, Integer size);

    /** 用户标记已读 */
    void markRead(Long userId, Long noticeId);

    /** 用户未读数量 */
    long unreadCount(Long userId);
}
