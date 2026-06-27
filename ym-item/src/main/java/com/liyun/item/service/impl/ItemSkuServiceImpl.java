package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.common.utils.BeanUtils;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.pojo.ItemSku;
import com.liyun.item.mapper.ItemSkuMapper;
import com.liyun.item.service.IItemService;
import com.liyun.item.service.IItemSkuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemSkuServiceImpl extends ServiceImpl<ItemSkuMapper, ItemSku> implements IItemSkuService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemSkuServiceImpl.class);

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
    @Transactional
    public void batchDeductStock(Map<Long, Integer> skuQtyMap) {
        if (CollectionUtils.isEmpty(skuQtyMap)) return;

        List<ItemSku> skus = listByIds(skuQtyMap.keySet());
        for (ItemSku sku : skus) {
            Integer qty = skuQtyMap.get(sku.getId());
            if (qty == null || qty <= 0) continue;
            if (sku.getStock() == null || sku.getStock() < qty) {
                throw new RuntimeException("SKU库存不足: skuId=" + sku.getId() + ", 库存=" + sku.getStock() + ", 需要=" + qty);
            }
            sku.setStock(sku.getStock() - qty);
            sku.setUpdateTime(LocalDateTime.now());
            updateById(sku);
            LOGGER.info("[SKU] 扣减库存 skuId={}, 扣减={}, 剩余={}", sku.getId(), qty, sku.getStock());
        }
    }

    @Override
    @Transactional
    public void batchAddSold(Map<Long, Integer> itemQtyMap) {
        if (CollectionUtils.isEmpty(itemQtyMap)) return;

        for (Map.Entry<Long, Integer> entry : itemQtyMap.entrySet()) {
            Long itemId = entry.getKey();
            Integer qty = entry.getValue();
            if (qty == null || qty <= 0) continue;

            Item item = itemService.getById(itemId);
            if (item == null) {
                LOGGER.warn("[SKU] batchAddSold: itemId={} 不存在", itemId);
                continue;
            }
            int newSold = (item.getSold() != null ? item.getSold() : 0) + qty;
            item.setSold(newSold);
            item.setUpdateTime(LocalDateTime.now());
            itemService.updateById(item);
            LOGGER.info("[SKU] 增加销量 itemId={}, 增加={}, 总计={}", itemId, qty, newSold);
        }
    }
}
