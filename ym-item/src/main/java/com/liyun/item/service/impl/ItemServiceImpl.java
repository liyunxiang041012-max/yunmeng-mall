package com.liyun.item.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.json.JsonData;
import com.alibaba.nacos.common.utils.StringUtils;

import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.liyun.api.dto.ItemInfoDTO;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.CollUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.doc.ItemDoc;
import com.liyun.item.domain.pojo.Brand;
import com.liyun.item.domain.pojo.Category;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.domain.pojo.Shop;
import com.liyun.item.domain.vo.ItemDetailVO;
import com.liyun.item.domain.vo.ItemVO;
import com.liyun.item.mapper.BrandMapper;
import com.liyun.item.mapper.CategoryMapper;
import com.liyun.item.mapper.ItemMapper;
import com.liyun.item.mapper.ShopMapper;
import com.liyun.item.query.ItemPageQuery;

import com.liyun.item.repository.ItemRepository;
import com.liyun.item.service.CategoryCacheService;
import com.liyun.item.service.IItemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * <p>
 * 商品SPU 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {


    private final ElasticsearchOperations esTemplate;


    private final ShopMapper shopMapper;


    private final CategoryMapper categoryMapper;


    private final BrandMapper brandMapper;
    private final CategoryCacheService categoryCacheService;
    private final ItemRepository repository;
    /**
     * 临时用：把MySQL数据全量同步到ES
     */
    public void syncToEs() {
        List<Item> items = list();
        if (CollUtils.isEmpty(items)) return;

        // 批量查，不要在循环里单条查
        List<Long> shopIds = items.stream().map(Item::getShopId).distinct().collect(Collectors.toList());
        List<Long> categoryIds = items.stream().map(Item::getCategoryId).distinct().collect(Collectors.toList());
        List<Long> brandIds = items.stream()
                .map(Item::getBrandId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> shopMap = shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getShopName));
        Map<Long, String> categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, String> brandMap = brandIds.isEmpty() ? Collections.emptyMap() :
                brandMapper.selectBatchIds(brandIds).stream()
                        .collect(Collectors.toMap(Brand::getId, Brand::getName));

        List<ItemDoc> docs = items.stream().map(item -> {
            ItemDoc doc = BeanUtils.copyBean(item, ItemDoc.class);
            doc.setShopName(shopMap.get(item.getShopId()));
            doc.setCategoryName(categoryMap.get(item.getCategoryId()));
            if (item.getBrandId() != null) {
                doc.setBrandName(brandMap.get(item.getBrandId()));
            }
            return doc;
        }).collect(Collectors.toList());

        // 分批写入ES，不要一次性saveAll，数据量大会OOM
        Lists.partition(docs, 500).forEach(repository::saveAll);
    }



    @Override
    public PageDTO<ItemVO> pageQuery(ItemPageQuery query) {
        // 1. 构建查询条件
        NativeQueryBuilder queryBuilder = NativeQuery.builder();

        // 关键词搜索
        if (StringUtils.hasText(query.getKeyword())) {
            queryBuilder.withQuery(q -> q
                    .match(m -> m
                            .field("name")
                            .query(query.getKeyword())
                    )
            );
        } else {
            queryBuilder.withQuery(q -> q.matchAll(m -> m));
        }

        // 过滤条件
        List<Query> filters = new ArrayList<>();

        // 分类过滤 - 从Redis展开
        if (query.getCategoryId() != null) {
            List<Long> categoryIds = categoryCacheService
                    .getAllChildCategoryIds(query.getCategoryId());

            filters.add(Query.of(q -> q
                    .terms(t -> t
                            .field("categoryId")
                            .terms(v -> v.value(categoryIds.stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList())))
                    )
            ));
        }

        // 品牌过滤
        if (query.getBrandId() != null) {
            filters.add(Query.of(q -> q
                    .term(t -> t.field("brandId").value(query.getBrandId()))
            ));
        }

        // 价格区间
        if (query.getMinPrice() != null || query.getMaxPrice() != null) {
            filters.add(Query.of(q -> q
                    .range(r -> {
                        r.field("price");
                        if (query.getMinPrice() != null) r.gte(JsonData.of(query.getMinPrice()));
                        if (query.getMaxPrice() != null) r.lte(JsonData.of(query.getMaxPrice()));
                        return r;
                    })
            ));
        }

        // 店铺过滤
        if (query.getShopId() != null) {
            filters.add(Query.of(q -> q
                    .term(t -> t.field("shopId").value(query.getShopId()))
            ));
        }

        // 有货过滤
        if (Boolean.TRUE.equals(query.getInStock())) {
            filters.add(Query.of(q -> q
                    .range(r -> r.field("stock").gt(JsonData.of(0)))
            ));
        }

        // status = 1 只查上架
        filters.add(Query.of(q -> q
                .term(t -> t.field("status").value(1))
        ));

        if (!filters.isEmpty()) {
            queryBuilder.withFilter(f -> f
                    .bool(b -> b.filter(filters))
            );
        }

        // 2. 排序
        String sortBy = query.getSortBy();
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                case "price" -> queryBuilder.withSort(s -> s
                        .field(f -> f.field("price").order(query.getIsAsc() ? SortOrder.Asc : SortOrder.Desc))
                );
                case "sold" -> queryBuilder.withSort(s -> s
                        .field(f -> f.field("sold").order(SortOrder.Desc))
                );
                default -> queryBuilder.withSort(s -> s
                        .field(f -> f.field("id").order(SortOrder.Desc))
                );
            }
        }

        // 3. 分页
        queryBuilder.withPageable(PageRequest.of(query.getPageNo() - 1, query.getPageSize()));

        // 4. 执行查询
        SearchHits<ItemDoc> hits = esTemplate.search(queryBuilder.build(), ItemDoc.class);

        // 5. 封装结果
        long total = hits.getTotalHits();
        if (total == 0) return new PageDTO<>(0L, 0L, CollUtils.emptyList());

        List<ItemVO> list = hits.getSearchHits().stream()
                .map(hit -> BeanUtils.copyBean(hit.getContent(), ItemVO.class))
                .collect(Collectors.toList());

        long pages = (total + query.getPageSize() - 1) / query.getPageSize();
        return new PageDTO<>(total, pages, list);
    }

    @Override
    public ItemInfoDTO getItemInfo(Long itemId) {
        Item item = getById(itemId);
        if (item == null) {
            return null;
        }

        ItemInfoDTO dto = new ItemInfoDTO();
        dto.setId(item.getId());
        dto.setShopId(item.getShopId());
        dto.setCategoryId(item.getCategoryId());
        dto.setBrandId(item.getBrandId());
        dto.setName(item.getName());
        dto.setImage(item.getImage());
        dto.setPrice(item.getPrice());
        dto.setStock(item.getStock());
        dto.setSold(item.getSold());
        dto.setStatus(item.getStatus());

        return dto;
    }

    @Override
    public List<ItemInfoDTO> batchGetItemInfo(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Item> items = list(new LambdaQueryWrapper<Item>()
                .in(Item::getId, itemIds));

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream().map(item -> {
            ItemInfoDTO dto = new ItemInfoDTO();
            dto.setId(item.getId());
            dto.setShopId(item.getShopId());
            dto.setCategoryId(item.getCategoryId());
            dto.setBrandId(item.getBrandId());
            dto.setName(item.getName());
            dto.setImage(item.getImage());
            dto.setPrice(item.getPrice());
            dto.setStock(item.getStock());
            dto.setSold(item.getSold());
            dto.setStatus(item.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }
}
