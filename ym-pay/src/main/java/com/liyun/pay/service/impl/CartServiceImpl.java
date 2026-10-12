package com.liyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.api.client.ItemFeign;
import com.liyun.api.client.ShopFeign;
import com.liyun.api.dto.SkuInfoDTO;
import com.liyun.api.vo.ShopCartVO;
import com.liyun.common.context.UserContext;
import com.liyun.pay.domain.dto.CartDTO;
import com.liyun.pay.domain.pojo.Cart;
import com.liyun.pay.domain.vo.CartVO;
import com.liyun.pay.mapper.CartMapper;
import com.liyun.pay.service.ICartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    private final ItemFeign itemFeign;
    private final ShopFeign shopFeign;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    private <T> T extractData(Map<String, Object> result, Class<T> clazz) {
        Object data = result.get("data");
        if (data == null) return null;
        return objectMapper.convertValue(data, clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> extractDataList(Map<String, Object> result, Class<T> clazz) {
        Object data = result.get("data");
        if (!(data instanceof List)) return Collections.emptyList();
        return ((List<Object>) data).stream()
                .map(item -> objectMapper.convertValue(item, clazz))
                .collect(Collectors.toList());
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    @Override
    @Transactional
    public void addCart(CartDTO dto) {
        Long userId = getCurrentUserId();
        Long skuId = dto.getSkuId();

        SkuInfoDTO sku = extractData(itemFeign.getSkuInfo(skuId), SkuInfoDTO.class);
        if (sku == null) {
            throw new RuntimeException("商品不存在");
        }

        LambdaQueryWrapper<Cart> query = new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, skuId);

        Cart existing = this.getOne(query);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            existing.setUpdateTime(LocalDateTime.now());
            this.updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setSkuId(skuId);
            cart.setSpuId(sku.getItemId());
            cart.setShopId(sku.getShopId());
            cart.setName(sku.getName());
            cart.setImage(sku.getImage() != null ? sku.getImage() : "");
            cart.setSpecInfo(sku.getName());
            cart.setPrice(Long.valueOf(sku.getPrice()));
            cart.setQuantity(dto.getQuantity());
            cart.setSelected(false);
            cart.setCreateTime(LocalDateTime.now());
            cart.setUpdateTime(LocalDateTime.now());
            this.save(cart);
        }
    }
    @Override
    public List<CartVO> cartList() {
        Long userId = getCurrentUserId();
        List<Cart> carts = this.list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .gt(Cart::getQuantity, 0));

        if (carts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> skuIds = carts.stream().map(Cart::getSkuId).distinct().collect(Collectors.toList());
        List<Long> shopIds = carts.stream().map(Cart::getShopId).distinct().collect(Collectors.toList());

        // 两个Feign并行调用
        CompletableFuture<Map<Long, SkuInfoDTO>> skuFuture = CompletableFuture.supplyAsync(() -> {
            List<SkuInfoDTO> list = extractDataList(itemFeign.batchGetSkuInfo(skuIds), SkuInfoDTO.class);
            if (list != null) {
                return list.stream()
                        .collect(Collectors.toMap(SkuInfoDTO::getId, Function.identity()));
            }
            return Collections.emptyMap();
        });

        CompletableFuture<Map<Long, ShopCartVO>> shopFuture = CompletableFuture.supplyAsync(() -> {
            List<ShopCartVO> list = extractDataList(shopFeign.batchGetShop(shopIds), ShopCartVO.class);
            if (list != null) {
                return list.stream()
                        .collect(Collectors.toMap(ShopCartVO::getId, Function.identity()));
            }
            return Collections.emptyMap();
        });

        Map<Long, SkuInfoDTO> skuMap;
        Map<Long, ShopCartVO> shopMap;
        try {
            skuMap = skuFuture.get();
            shopMap = shopFuture.get();
        } catch (Exception e) {
            log.error("购物车查询远程服务失败", e);
            skuMap = Collections.emptyMap();
            shopMap = Collections.emptyMap();
        }

        final Map<Long, SkuInfoDTO> finalSkuMap = skuMap;
        final Map<Long, ShopCartVO> finalShopMap = shopMap;

        List<Cart> toUpdate = new ArrayList<>();

        List<CartVO> result = carts.stream().map(cart -> {
            CartVO vo = new CartVO();
            vo.setId(cart.getId());
            vo.setUserId(cart.getUserId());
            vo.setSkuId(cart.getSkuId());
            vo.setShopId(cart.getShopId());
            vo.setQuantity(cart.getQuantity());
            vo.setSelected(cart.getSelected());
            vo.setCreateTime(cart.getCreateTime());
            vo.setSnapshotPrice(cart.getPrice()); // Long，不转int

            SkuInfoDTO sku = finalSkuMap.get(cart.getSkuId());
            if (sku != null) {
                vo.setSkuName(sku.getName());
                vo.setPrice(Long.valueOf(sku.getPrice()));
                vo.setImage(sku.getImage());
                vo.setOffShelf(false);

                // 统一用longValue比对，避免类型不一致
                if (!cart.getPrice().equals(sku.getPrice().longValue())) {
                    vo.setPriceChanged(true);
                    cart.setPrice(sku.getPrice().longValue());
                    cart.setUpdateTime(LocalDateTime.now());
                    toUpdate.add(cart);
                } else {
                    vo.setPriceChanged(false);
                }
            } else {
                // 商品查不到，标记下架
                vo.setPrice(cart.getPrice());
                vo.setPriceChanged(false);
                vo.setOffShelf(true);
            }

            ShopCartVO shop = finalShopMap.get(cart.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getName());
            } else {
                vo.setShopName("商家" + cart.getShopId());
            }

            return vo;
        }).collect(Collectors.toList());

        // 异步更新快照价格，不阻塞接口返回
        if (!toUpdate.isEmpty()) {
            CompletableFuture.runAsync(() -> this.updateBatchById(toUpdate));
        }

        return result;
    }

    @Override
    @Transactional
    public void updateCart(CartDTO dto) {
        Long userId = getCurrentUserId();
        Cart cart = this.getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, dto.getSkuId()));
        if (cart == null) {
            throw new RuntimeException("购物车商品不存在");
        }
        cart.setQuantity(dto.getQuantity());
        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    @Override
    @Transactional
    public void deleteCart(List<Long> ids) {
        Long userId = getCurrentUserId();
        this.remove(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, ids));
    }

    @Override
    @Transactional
    public void reduceCartAfterOrder(Long userId, Map<Long, Integer> skuQuantityMap) {
        if (skuQuantityMap == null || skuQuantityMap.isEmpty()) return;

        List<Long> skuIds = new ArrayList<>(skuQuantityMap.keySet());
        List<Cart> cartItems = this.list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getSkuId, skuIds));

        if (cartItems.isEmpty()) return;

        for (Cart cart : cartItems) {
            int orderedQty = skuQuantityMap.getOrDefault(cart.getSkuId(), 0);
            int remaining = Math.max(0, cart.getQuantity() - orderedQty);
            log.info("【购物车清除】skuId={}, 原数量={}, 购买={}, 剩余={}",
                    cart.getSkuId(), cart.getQuantity(), orderedQty, remaining);
            cart.setQuantity(remaining);
            cart.setUpdateTime(LocalDateTime.now());
        }
        this.updateBatchById(cartItems);
        log.info("【购物车清除】扣减 {} 个商品: userId={}, skuIds={}", cartItems.size(), userId, skuIds);
    }
}
