package com.liyun.item.service;

import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.History;
import com.liyun.item.domain.vo.HistoryItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户浏览历史 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
public interface IHistoryService extends IService<History> {

    /**
     * 添加浏览记录
     * @param userId 用户ID
     * @param itemId 商品ID
     */
    void addHistory(Long userId, Long itemId);

    /**
     * 获取用户浏览历史（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页条数
     * @return 浏览历史分页
     */
    PageDTO<HistoryItemVO> getMyHistory(Long userId, Integer page, Integer size);

    /**
     * 删除单条浏览记录
     * @param userId 用户ID
     * @param historyId 浏览记录ID
     */
    void deleteHistory(Long userId, Long historyId);

    /**
     * 清空用户全部浏览记录
     * @param userId 用户ID
     */
    void clearHistory(Long userId);
}
