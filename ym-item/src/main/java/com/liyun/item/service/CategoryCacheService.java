package com.liyun.item.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyun.item.domain.pojo.Category;
import com.liyun.item.mapper.CategoryMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryCacheService {

    private final StringRedisTemplate redisTemplate;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    private static final String CATEGORY_CHILDREN_KEY = "category:children";

    @PostConstruct
    public void loadCategoryCache() {
        List<Category> all = categoryMapper.selectList(null);

        Map<Long, List<Long>> parentChildMap = all.stream()
                .collect(Collectors.groupingBy(
                        Category::getParentId,
                        Collectors.mapping(Category::getId, Collectors.toList())
                ));

        Map<String, String> hashMap = new HashMap<>();
        parentChildMap.forEach((parentId, childIds) -> {
            try {
                hashMap.put(String.valueOf(parentId), objectMapper.writeValueAsString(childIds));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        redisTemplate.opsForHash().putAll(CATEGORY_CHILDREN_KEY, hashMap);
    }

    public List<Long> getAllChildCategoryIds(Long categoryId) {
        List<Long> result = new ArrayList<>();
        result.add(categoryId);
        collectFromRedis(categoryId, result);
        return result;
    }

    private void collectFromRedis(Long parentId, List<Long> result) {
        Object value = redisTemplate.opsForHash()
                .get(CATEGORY_CHILDREN_KEY, String.valueOf(parentId));

        if (value == null) return;

        try {
            List<Long> childIds = objectMapper.readValue(
                    value.toString(),
                    new TypeReference<List<Long>>() {}
            );
            for (Long childId : childIds) {
                result.add(childId);
                collectFromRedis(childId, result);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void refreshCache() {
        redisTemplate.delete(CATEGORY_CHILDREN_KEY);
        loadCategoryCache();
    }
}