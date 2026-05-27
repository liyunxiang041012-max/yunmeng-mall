package com.liyun.item.service;

import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.item.domain.pojo.ItemSku;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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

    /**
     * 扣减库存
     * @param skuId SKU ID
     * @param quantity 扣减数量
     */
    void deductStock(Long skuId, Integer quantity);
}
