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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemSkuServiceImpl extends ServiceImpl<ItemSkuMapper, ItemSku> implements IItemSkuService {

    private final IItemService itemService;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SKU_STOCK_LOCK_KEY = "lock:sku:stock:";
    private static final String SKU_CACHE_KEY = "cache:sku:info:";
    private static final long SKU_CACHE_TTL = 10; // 分钟

    @Override
    public SkuInfoDTO getSkuInfo(Long skuId) {
        // 1. 查Redis缓存
        String cacheKey = SKU_CACHE_KEY + skuId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SkuInfoDTO.class);
            } catch (Exception e) {
                log.warn("SKU缓存反序列化失败，skuId: {}", skuId, e);
            }
        }

        // 2. 查DB
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

        // 3. 写入Redis缓存
        try {
            redisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(dto), SKU_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("SKU缓存写入失败，skuId: {}", skuId, e);
        }

        return dto;
    }

    @Override
    public List<SkuInfoDTO> batchGetSkuInfo(List<Long> skuIds) {
        if (CollectionUtils.isEmpty(skuIds)) {
            return Collections.emptyList();
        }

        List<SkuInfoDTO> result = new ArrayList<>();
        List<Long> missedIds = new ArrayList<>();

        // 1. 批量查Redis缓存
        for (Long skuId : skuIds) {
            String cacheKey = SKU_CACHE_KEY + skuId;
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    result.add(objectMapper.readValue(cached, SkuInfoDTO.class));
                } catch (Exception e) {
                    log.warn("SKU缓存反序列化失败，skuId: {}", skuId, e);
                    missedIds.add(skuId);
                }
            } else {
                missedIds.add(skuId);
            }
        }

        // 2. 缓存未命中的查DB
        if (!missedIds.isEmpty()) {
            List<ItemSku> skus = list(new LambdaQueryWrapper<ItemSku>()
                    .in(ItemSku::getId, missedIds));

            if (!CollectionUtils.isEmpty(skus)) {
                List<Long> itemIds = skus.stream()
                        .map(ItemSku::getItemId)
                        .distinct()
                        .collect(Collectors.toList());

                List<Item> items = itemService.list(new LambdaQueryWrapper<Item>()
                        .in(Item::getId, itemIds));

                Map<Long, Item> itemMap = items.stream()
                        .collect(Collectors.toMap(Item::getId, Function.identity()));

                for (ItemSku sku : skus) {
                    Item item = itemMap.get(sku.getItemId());
                    if (item == null) continue;

                    SkuInfoDTO dto = new SkuInfoDTO();
                    dto.setId(sku.getId());
                    dto.setItemId(sku.getItemId());
                    dto.setShopId(item.getShopId());
                    dto.setName(sku.getSkuName() != null ? sku.getSkuName() : item.getName());
                    dto.setPrice(sku.getPrice().intValue());
                    dto.setImage(sku.getImage() != null ? sku.getImage() : item.getImage());
                    dto.setStock(sku.getStock());
                    result.add(dto);

                    // 3. 写入Redis缓存
                    try {
                        String cacheKey = SKU_CACHE_KEY + sku.getId();
                        redisTemplate.opsForValue().set(cacheKey,
                                objectMapper.writeValueAsString(dto), SKU_CACHE_TTL, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.warn("SKU缓存写入失败，skuId: {}", sku.getId(), e);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void deductStock(Long skuId, Integer quantity) {
        String lockKey = SKU_STOCK_LOCK_KEY + skuId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BizException(ResultCode.FAIL, "系统繁忙，请稍后再试");
            }
            try {
                ItemSku sku = getById(skuId);
                if (sku == null) {
                    throw new BizException(ResultCode.NOT_FOUND, "商品SKU不存在");
                }
                if (sku.getStock() == null || sku.getStock() < quantity) {
                    throw new BizException(ResultCode.FAIL, "库存不足，skuId：" + skuId);
                }
                sku.setStock(sku.getStock() - quantity);
                updateById(sku);
                // 清除SKU缓存，确保库存数据一致
                redisTemplate.delete(SKU_CACHE_KEY + skuId);
                log.info("扣减库存成功，skuId: {}, 扣减数量: {}, 剩余库存: {}", skuId, quantity, sku.getStock());
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.FAIL, "系统繁忙，请稍后再试");
        }
    }
}
