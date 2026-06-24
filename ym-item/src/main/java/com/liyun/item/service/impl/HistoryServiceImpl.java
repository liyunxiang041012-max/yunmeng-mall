package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.exception.BizException;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.History;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.vo.HistoryItemVO;
import com.liyun.item.mapper.HistoryMapper;
import com.liyun.item.service.IHistoryService;
import com.liyun.item.service.IItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户浏览历史 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl extends ServiceImpl<HistoryMapper, History> implements IHistoryService {

    private final IItemService itemService;

    @Override
    public void addHistory(Long userId, Long itemId) {
        // 校验用户ID
        if (userId == null) {
            return; // 未登录用户不记录浏览历史
        }

        // 先删除该用户对该商品的旧浏览记录（避免重复）
        remove(new LambdaQueryWrapper<History>()
                .eq(History::getUserId, userId)
                .eq(History::getItemId, itemId)
        );

        // 添加新的浏览记录
        History history = new History();
        history.setUserId(userId);
        history.setItemId(itemId);
        save(history);
    }

    @Override
    public PageDTO<HistoryItemVO> getMyHistory(Long userId, Integer page, Integer size) {
        // 校验用户ID
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }

        // 1. 分页查询浏览历史
        LambdaQueryWrapper<History> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(History::getUserId, userId)
               .orderByDesc(History::getCreateTime);

        Page<History> pageParam = new Page<>(page, size);
        Page<History> historyPage = page(pageParam, wrapper);

        if (historyPage.getRecords().isEmpty()) {
            return PageDTO.empty(historyPage);
        }

        // 2. 获取浏览的商品ID列表
        List<Long> itemIds = historyPage.getRecords().stream()
                .map(History::getItemId)
                .collect(Collectors.toList());

        // 3. 批量查询商品信息
        List<Item> items = itemService.listByIds(itemIds);

        // 4. 组装返回数据
        List<HistoryItemVO> voList = historyPage.getRecords().stream().map(hist -> {
            HistoryItemVO vo = new HistoryItemVO();
            vo.setId(hist.getId());
            vo.setItemId(hist.getItemId());
            vo.setCreateTime(hist.getCreateTime());
            vo.setViewTime(hist.getCreateTime());

            // 查找对应的商品信息
            Item item = items.stream()
                    .filter(i -> i.getId().equals(hist.getItemId()))
                    .findFirst()
                    .orElse(null);

            if (item != null) {
                vo.setName(item.getName());
                vo.setItemName(item.getName());
                vo.setMainImage(item.getImage());
                vo.setImage(item.getImage());
                vo.setPrice(item.getPrice());
            }

            return vo;
        }).collect(Collectors.toList());

        return PageDTO.of(historyPage, voList);
    }

    @Override
    public void deleteHistory(Long userId, Long historyId) {
        // 校验用户ID
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 删除指定记录（确保是该用户的记录）
        remove(new LambdaQueryWrapper<History>()
                .eq(History::getId, historyId)
                .eq(History::getUserId, userId)
        );
    }

    @Override
    public void clearHistory(Long userId) {
        // 校验用户ID
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 清空该用户的所有浏览记录
        remove(new LambdaQueryWrapper<History>()
                .eq(History::getUserId, userId)
        );
    }
}
