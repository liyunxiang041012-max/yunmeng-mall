package com.liyun.item.service;

import com.liyun.api.dto.ItemInfoDTO;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.pojo.Item;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.item.domain.vo.ItemVO;
import com.liyun.item.query.ItemPageQuery;

import java.util.List;
import java.util.Map;

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

    /** ======== 商家商品管理 ======== */

    Long getCurrentShopId();

    PageDTO<Map<String, Object>> listMyItems(Integer page, Integer size, Integer status, String keyword);

    Item saveItem(Item item);

    Item saveItem(Item item, List<String> specNames, List<Map<String, Object>> skuList);

    void updateItem(Long itemId, Item item);

    void updateItem(Long itemId, Item item, List<String> specNames, List<Map<String, Object>> skuList);

    void deleteItem(Long itemId);

    void toggleItemStatus(Long itemId);

    /** ======== 管理员商品审核 ======== */

    /** 管理员分页查询所有商品（含店铺名，支持 status 和 auditStatus 筛选） */
    PageDTO<Map<String, Object>> listAllItems(Integer page, Integer size, Integer status, Integer auditStatus, String keyword);

    /** 审核通过（上架） */
    void approveItem(Long itemId);

    /** 审核驳回（下架） */
    void rejectItem(Long itemId);
    /** 管理员 - 上下架商品（仅 auditStatus=1 时可操作，下架时退回待审核） */
    void adminToggleStatus(Long itemId);
}
