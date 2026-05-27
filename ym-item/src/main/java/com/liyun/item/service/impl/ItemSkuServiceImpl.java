package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.exception.BizException;
import com.liyun.common.utils.BeanUtils;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.pojo.ItemSku;
import com.liyun.item.mapper.ItemSkuMapper;
import com.liyun.item.service.IItemService;
import com.liyun.item.service.IItemSkuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemSkuServiceImpl extends ServiceImpl<ItemSkuMapper, ItemSku> implements IItemSkuService {

    private final IItemService itemService;

    @Override
    public SkuInfoDTO getSkuInfo(Long skuId) {
        ItemSku sku = getById(skuId);
        if (sku == null) {
            return null;
        }

        Item item = itemService.getById(sku.getItemId());
        if (item == null) {
            return null;
        }

        SkuInfoDTO dto = new SkuInfoDTO();
        dto.setId(sku.getId());
        dto.setItemId(sku.getItemId());
        dto.setShopId(item.getShopId());
        dto.setName(sku.getSkuName() != null ? sku.getSkuName() : item.getName());
        dto.setPrice(sku.getPrice().intValue());
        dto.setImage(sku.getImage() != null ? sku.getImage() : item.getImage());
        dto.setStock(sku.getStock());

        return dto;
    }

    @Override
    public List<SkuInfoDTO> batchGetSkuInfo(List<Long> skuIds) {
        if (CollectionUtils.isEmpty(skuIds)) {
            return Collections.emptyList();
        }

        List<ItemSku> skus = list(new LambdaQueryWrapper<ItemSku>()
                .in(ItemSku::getId, skuIds));

        if (CollectionUtils.isEmpty(skus)) {
            return Collections.emptyList();
        }

        List<Long> itemIds = skus.stream()
                .map(ItemSku::getItemId)
                .distinct()
                .collect(Collectors.toList());

        List<Item> items = itemService.list(new LambdaQueryWrapper<Item>()
                .in(Item::getId, itemIds));

        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        return skus.stream().map(sku -> {
            Item item = itemMap.get(sku.getItemId());
            if (item == null) {
                return null;
            }

            SkuInfoDTO dto = new SkuInfoDTO();
            dto.setId(sku.getId());
            dto.setItemId(sku.getItemId());
            dto.setShopId(item.getShopId());
            dto.setName(sku.getSkuName() != null ? sku.getSkuName() : item.getName());
            dto.setPrice(sku.getPrice().intValue());
            dto.setImage(sku.getImage() != null ? sku.getImage() : item.getImage());
            dto.setStock(sku.getStock());

            return dto;
        }).filter(dto -> dto != null).collect(Collectors.toList());
    }

    @Override
    public void deductStock(Long skuId, Integer quantity) {
        ItemSku sku = getById(skuId);
        if (sku == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品SKU不存在");
        }
        if (sku.getStock() == null || sku.getStock() < quantity) {
            throw new BizException(ResultCode.FAIL, "库存不足，skuId：" + skuId);
        }
        sku.setStock(sku.getStock() - quantity);
        updateById(sku);
    }
}
