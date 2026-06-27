package com.liyun.item.service;

import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.item.domain.pojo.ItemSku;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * SKU 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
public interface IItemSkuService extends IService<ItemSku> {

    SkuInfoDTO getSkuInfo(Long skuId);

    List<SkuInfoDTO> batchGetSkuInfo(List<Long> skuIds);

    /** 批量扣减库存 key=skuId, value=扣减数量 */
    void batchDeductStock(Map<Long, Integer> skuQtyMap);

    /** 批量增加SPU销量 key=itemId, value=增加数量 */
    void batchAddSold(Map<Long, Integer> itemQtyMap);
}
