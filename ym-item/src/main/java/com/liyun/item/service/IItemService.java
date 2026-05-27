package com.liyun.item.service;

import com.liyun.api.dto.ItemInfoDTO;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.Item;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.item.domain.vo.ItemDetailVO;
import com.liyun.item.domain.vo.ItemVO;
import com.liyun.item.query.ItemPageQuery;

import java.util.List;

/**
 * <p>
 * 商品SPU 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
public interface IItemService extends IService<Item> {

    PageDTO<ItemVO> pageQuery(ItemPageQuery query);

    void syncToEs();

    ItemInfoDTO getItemInfo(Long itemId);

    List<ItemInfoDTO> batchGetItemInfo(List<Long> itemIds);
}
