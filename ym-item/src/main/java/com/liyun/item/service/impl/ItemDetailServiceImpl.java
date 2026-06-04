package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyun.common.context.UserContext;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.pojo.ItemDetail;
import com.liyun.item.domain.pojo.ItemSku;
import com.liyun.item.domain.vo.ItemDetailVO;
import com.liyun.item.domain.vo.ItemDetailVO.SkuVO;
import com.liyun.item.domain.vo.ItemDetailVO.SpecGroupVO;
import com.liyun.item.domain.vo.ItemDetailVO.SpecValueVO;
import com.liyun.item.mapper.ItemDetailMapper;
import com.liyun.item.mapper.ItemMapper;
import com.liyun.item.mapper.ItemSkuMapper;
import com.liyun.item.service.IHistoryService;
import com.liyun.item.service.IItemDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemDetailServiceImpl extends ServiceImpl<ItemDetailMapper, ItemDetail>
        implements IItemDetailService {

    private final ItemMapper itemMapper;
    private final ItemSkuMapper itemSkuMapper;
    private final ItemDetailMapper itemDetailMapper;
    private final ObjectMapper objectMapper;
    private final IHistoryService historyService;

    @Override
    public ItemDetailVO getItemDetail(Long itemId) {
        // 1. 查商品主表
        Item item = itemMapper.selectById(itemId);
        if (item == null) throw new RuntimeException("商品不存在");

        // 记录浏览历史（异步，不阻塞主流程）
        try {
            Long userId = UserContext.getUserId();
            if (userId != null) {
                historyService.addHistory(userId, itemId);
            }
        } catch (Exception e) {
            // 记录日志但不影响主流程
        }

        // 2. 查 SKU 列表
        List<ItemSku> skus = itemSkuMapper.selectList(
                new LambdaQueryWrapper<ItemSku>()
                        .eq(ItemSku::getItemId, itemId)
                        .eq(ItemSku::getStatus, 1)
                        .eq(ItemSku::getDeleted, 0)
        );

        // 3. 查商品详情（按 itemId 查，不是主键）
        ItemDetail detail = itemDetailMapper.selectOne(
                new LambdaQueryWrapper<ItemDetail>()
                        .eq(ItemDetail::getItemId, itemId)
        );

        // 4. 组装主信息
        ItemDetailVO vo = new ItemDetailVO();
        vo.setId(item.getId());
        vo.setShopId(item.getShopId());
        vo.setName(item.getName());
        vo.setSold(item.getSold());
        vo.setMainImage(item.getImage());
        vo.setImages(parseImages(item.getImage()));

        // 最低价
        long minPrice = skus.stream().mapToLong(ItemSku::getPrice).min().orElse(0L);
        vo.setPrice(minPrice);

        // 5. 详情字段，直接对齐实体字段名
        if (detail != null) {
            vo.setDescription(detail.getDescription());   // 图文描述富文本
            vo.setDetailImgs(detail.getDetailImgs());     // 详情图片列表
        }

        // 6. 组装 SKU VO
        List<SkuVO> skuVOs = skus.stream().map(sku -> {
            SkuVO skuVO = new SkuVO();
            skuVO.setId(sku.getId());
            skuVO.setItemId(sku.getItemId());
            skuVO.setSkuName(sku.getSkuName());
            skuVO.setPrice(sku.getPrice());
            skuVO.setStock(sku.getStock());
            skuVO.setImage(sku.getImage());
            skuVO.setSpecData(parseSpecData(sku.getSpecData()));
            return skuVO;
        }).collect(Collectors.toList());
        vo.setSkus(skuVOs);

        // 7. 从 SKU specData 聚合规格组
        vo.setSpecs(buildSpecGroups(skuVOs));

        return vo;
    }

    /**
     * 解析 spec_data JSON → Map
     * 示例：{"颜色":"黑色","存储":"128G"}
     */
    private Map<String, String> parseSpecData(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 从 SKU 列表聚合规格组
     * 输出：[{specName:颜色, values:[{value:黑色,stock:true}, ...]}, ...]
     */
    private List<SpecGroupVO> buildSpecGroups(List<SkuVO> skuVOs) {
        LinkedHashMap<String, LinkedHashMap<String, Boolean>> groupMap = new LinkedHashMap<>();

        for (SkuVO sku : skuVOs) {
            boolean hasStock = sku.getStock() != null && sku.getStock() > 0;
            for (Map.Entry<String, String> entry : sku.getSpecData().entrySet()) {
                groupMap
                        .computeIfAbsent(entry.getKey(), k -> new LinkedHashMap<>())
                        .merge(entry.getValue(), hasStock, Boolean::logicalOr);
            }
        }

        List<SpecGroupVO> result = new ArrayList<>();
        groupMap.forEach((specName, valueMap) -> {
            SpecGroupVO group = new SpecGroupVO();
            group.setSpecName(specName);
            List<SpecValueVO> values = new ArrayList<>();
            valueMap.forEach((val, stock) -> {
                SpecValueVO v = new SpecValueVO();
                v.setValue(val);
                v.setStock(stock);
                values.add(v);
            });
            group.setValues(values);
            result.add(group);
        });
        return result;
    }

    /**
     * 解析图片字段：支持 JSON 数组 ["url1","url2"] 或逗号分隔 url1,url2
     */
    private List<String> parseImages(String imageStr) {
        if (imageStr == null || imageStr.isBlank()) return Collections.emptyList();
        if (imageStr.startsWith("[")) {
            try {
                return objectMapper.readValue(imageStr, new TypeReference<List<String>>() {});
            } catch (Exception ignored) {}
        }
        return Arrays.asList(imageStr.split(","));
    }
}