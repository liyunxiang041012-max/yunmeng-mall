package com.liyun.common.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollUtils {

    // List转Map
    public static <T, K> Map<K, T> toMap(List<T> list, Function<T, K> keyMapper) {
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (a, b) -> a));
    }

    // 提取字段列表
    public static <T, R> List<R> toList(List<T> list, Function<T, R> mapper) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    // 提取字段去重
    public static <T, R> Set<R> toSet(List<T> list, Function<T, R> mapper) {
        if (list == null || list.isEmpty()) return Collections.emptySet();
        return list.stream().map(mapper).collect(Collectors.toSet());
    }

    // 判断是否为空
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    // List分组
    public static <T, K> Map<K, List<T>> groupBy(List<T> list, Function<T, K> classifier) {
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        return list.stream().collect(Collectors.groupingBy(classifier));
    }

    public static <T> List<T> emptyList() {
        return Collections.emptyList();
    }
}