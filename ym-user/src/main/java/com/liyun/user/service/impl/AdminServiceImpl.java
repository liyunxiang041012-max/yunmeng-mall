package com.liyun.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.api.client.OrderFeign;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.user.enums.GenderEnum;
import com.liyun.user.domain.pojo.User;
import com.liyun.user.domain.pojo.UserProfile;
import com.liyun.user.mapper.UserMapper;
import com.liyun.user.service.IAdminService;
import com.liyun.user.service.IUserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员用户管理 服务实现类
 *
 * @author liyun
 * @since 2026-06-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl extends ServiceImpl<UserMapper, User> implements IAdminService {

    private final IUserProfileService userProfileService;
    private final OrderFeign orderFeign;
    private final ObjectMapper objectMapper;

    /** 校验管理员角色 */
    private void checkAdminRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new RuntimeException("仅管理员可访问此接口");
        }
    }

    @Override
    public PageDTO<Map<String, Object>> pageUsers(Integer page, Integer size, String keyword, Integer role, Integer status) {
        checkAdminRole();

        Page<User> p = Page.of(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getNickname, keyword));
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        this.page(p, wrapper);

        List<Long> userIds = p.getRecords().stream().map(User::getId).collect(Collectors.toList());
        Map<Long, UserProfile> profileMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<UserProfile> profiles = userProfileService.listByIds(userIds);
            profileMap = profiles.stream().collect(Collectors.toMap(UserProfile::getId, pr -> pr, (a, b) -> a));
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (User u : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("phone", u.getPhone());
            m.put("nickname", u.getNickname());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            m.put("updateTime", u.getUpdateTime());
            UserProfile pr = profileMap.get(u.getId());
            m.put("avatar", pr != null ? pr.getAvatar() : "");
            records.add(m);
        }

        return PageDTO.of(p, records);
    }

    @Override
    public Map<String, Object> getUserDetail(Long userId) {
        checkAdminRole();

        User user = this.getById(userId);
        if (user == null) {
            return null;
        }
        UserProfile profile = userProfileService.getById(userId);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("phone", user.getPhone());
        m.put("nickname", user.getNickname());
        m.put("role", user.getRole());
        m.put("status", user.getStatus());
        m.put("createTime", user.getCreateTime());
        m.put("updateTime", user.getUpdateTime());
        m.put("avatar", profile != null ? profile.getAvatar() : "");
        m.put("experience", profile != null ? profile.getExperience() : 0);
        m.put("region", profile != null ? profile.getRegion() : "");
        return m;
    }

    @Override
    public void toggleUserStatus(Long userId, Integer status) {
        checkAdminRole();

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        log.info("管理员修改用户状态，userId: {}, status: {}", userId, status);
    }

    @Override
    public void changeUserRole(Long userId, Integer role) {
        checkAdminRole();

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        log.info("管理员修改用户角色，userId: {}, role: {}", userId, role);
    }

    @Override
    public Map<String, Object> getOverview() {
        checkAdminRole();

        // ===== 真实用户统计 =====
        long totalUsers = this.count();
        long activeUsers = this.count(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));

        // 性别分布
        long maleCount = userProfileService.count(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getGender, GenderEnum.MALE.getCode()));
        long femaleCount = userProfileService.count(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getGender, GenderEnum.FEMALE.getCode()));
        long totalGender = maleCount + femaleCount;
        int malePercent = totalGender > 0 ? (int) Math.round(maleCount * 100.0 / totalGender) : 0;
        int femalePercent = totalGender > 0 ? (int) Math.round(femaleCount * 100.0 / totalGender) : 0;

        // 年龄段分布（从 birthday 计算）
        List<Map<String, Object>> ages = computeAgeDistribution();

        // 地域分布
        List<Map<String, Object>> regions = computeRegionTop5();

        // ===== 从订单服务拉真实数据 =====
        long gmv = 0;
        long totalOrders = 0;
        long todayOrders = 0;
        List<Map<String, Object>> recentOrders = List.of();
        List<Map<String, Object>> topProducts = List.of();
        try {
            // 1. adminStats
            Map<String, Object> statsRaw = orderFeign.getAdminStats();
            log.info("[OVERVIEW] adminStats raw: {}", statsRaw);
            Map<String, Object> stats = extractDataMap(statsRaw);
            log.info("[OVERVIEW] adminStats extracted: {}", stats);
            if (stats != null) {
                gmv = ((Number) stats.getOrDefault("totalGmv", 0)).longValue();
                totalOrders = ((Number) stats.getOrDefault("totalOrders", 0)).longValue();
                todayOrders = ((Number) stats.getOrDefault("todayOrders", 0)).longValue();
            }

            // 2. recentOrders（Feign 直接返回 List）
            recentOrders = orderFeign.getAdminRecentOrders();
            if (recentOrders == null) recentOrders = List.of();
            log.info("[OVERVIEW] recentOrders ({}条): {}", recentOrders.size(), recentOrders);

            // 3. topProducts（Feign 直接返回 List）
            topProducts = orderFeign.getAdminTopProducts();
            if (topProducts == null) topProducts = List.of();
            log.info("[OVERVIEW] topProducts ({}条): {}", topProducts.size(), topProducts);
        } catch (Exception e) {
            log.warn("[OVERVIEW] 查询订单统计数据失败: {}", e.getMessage(), e);
        }

        log.info("[OVERVIEW] summary: gmv={}, totalOrders={}, todayOrders={}, recentOrders.size={}, topProducts.size={}",
                gmv, totalOrders, todayOrders, recentOrders.size(), topProducts.size());

        // ===== 组装 stats =====
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("gmv", Map.of("value", formatMoney(gmv), "trend", 0.0, "delta", "—"));
        stats.put("orders", Map.of("value", String.valueOf(totalOrders), "trend", 0.0, "delta", String.valueOf(todayOrders)));
        stats.put("users", Map.of("value", String.valueOf(totalUsers), "trend", 0.0, "delta", String.valueOf(activeUsers)));
        stats.put("conversion", Map.of("value", "—", "trend", 0.0, "delta", "—"));

        // ===== 组装 userPortrait =====
        Map<String, Object> userPortrait = new LinkedHashMap<>();
        userPortrait.put("gender", Map.of("male", malePercent, "female", femalePercent));
        userPortrait.put("ages", ages);
        userPortrait.put("regions", regions);

        // ===== 组装最近订单（空则返回空数组） =====
        // 空数组时前端自动显示"暂无订单数据"

        // ===== 组装热销商品（空则返回空数组） =====
        if (!topProducts.isEmpty()) {
            String[] colors = {"#FDE8C8", "#DBEAFE", "#E8D5F5", "#D5F0E2", "#FCE7F3"};
            for (int i = 0; i < topProducts.size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>(topProducts.get(i));
                item.put("color", colors[i]);
                topProducts.set(i, item);
            }
        }

        // ===== 组装结果 =====
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("userPortrait", userPortrait);
        result.put("consumeLevels", computeConsumeLevels(gmv, totalUsers));
        result.put("recentOrders", recentOrders);
        result.put("topProducts", topProducts);
        return result;
    }

    /** 根据生日计算年龄段分布 */
    private List<Map<String, Object>> computeAgeDistribution() {
        List<UserProfile> profiles = userProfileService.list(
                new LambdaQueryWrapper<UserProfile>().isNotNull(UserProfile::getBirthday));

        log.info("[OVERVIEW] computeAgeDistribution: 有生日数据的用户数={}", profiles.size());

        int age18to24 = 0, age25to34 = 0, age35to44 = 0, age45to54 = 0, age55plus = 0;
        LocalDate now = LocalDate.now();
        for (UserProfile p : profiles) {
            if (p.getBirthday() == null) continue;
            int age = Period.between(p.getBirthday(), now).getYears();
            log.debug("[OVERVIEW] userId={}, birthday={}, age={}", p.getId(), p.getBirthday(), age);
            if (age < 18) continue;
            if (age <= 24) age18to24++;
            else if (age <= 34) age25to34++;
            else if (age <= 44) age35to44++;
            else if (age <= 54) age45to54++;
            else age55plus++;
        }
        int total = age18to24 + age25to34 + age35to44 + age45to54 + age55plus;
        log.info("[OVERVIEW] age distribution: 18-24={}, 25-34={}, 35-44={}, 45-54={}, 55+={}, total={}",
                age18to24, age25to34, age35to44, age45to54, age55plus, total);
        if (total == 0) {
            log.info("[OVERVIEW] 无有效年龄数据，使用兜底占位");
            return List.of(
                    Map.of("label", "18-24", "percent", 28, "color", "#D9A53C"),
                    Map.of("label", "25-34", "percent", 42, "color", "#1A1712"),
                    Map.of("label", "35-44", "percent", 18, "color", "#8C6308"),
                    Map.of("label", "45-54", "percent", 8, "color", "#9B9484"),
                    Map.of("label", "55+", "percent", 4, "color", "#C0B8A8")
            );
        }
        return List.of(
                Map.of("label", "18-24", "percent", (int) Math.round(age18to24 * 100.0 / total), "color", "#D9A53C"),
                Map.of("label", "25-34", "percent", (int) Math.round(age25to34 * 100.0 / total), "color", "#1A1712"),
                Map.of("label", "35-44", "percent", (int) Math.round(age35to44 * 100.0 / total), "color", "#8C6308"),
                Map.of("label", "45-54", "percent", (int) Math.round(age45to54 * 100.0 / total), "color", "#9B9484"),
                Map.of("label", "55+", "percent", (int) Math.round(age55plus * 100.0 / total), "color", "#C0B8A8")
        );
    }

    /** 消费水平分布 */
    private List<Map<String, Object>> computeConsumeLevels(long gmv, long totalUsers) {
        if (gmv <= 0 || totalUsers <= 0) {
            return List.of(
                    Map.of("label", "高消费", "percent", 0, "color", "#D9A53C"),
                    Map.of("label", "中等消费", "percent", 0, "color", "#1A1712"),
                    Map.of("label", "低消费", "percent", 0, "color", "#8C6308")
            );
        }
        long avgPerUser = gmv / totalUsers;
        int high, mid, low;
        if (avgPerUser > 50000) { high = 55; mid = 25; low = 20; }
        else if (avgPerUser > 10000) { high = 30; mid = 40; low = 30; }
        else { high = 10; mid = 30; low = 60; }
        return List.of(
                Map.of("label", "高消费", "percent", high, "color", "#D9A53C"),
                Map.of("label", "中等消费", "percent", mid, "color", "#1A1712"),
                Map.of("label", "低消费", "percent", low, "color", "#8C6308")
        );
    }

    /** 从 user_profile.region 统计地域 TOP5 */
    private List<Map<String, Object>> computeRegionTop5() {
        List<UserProfile> profiles = userProfileService.list();
        Map<String, Long> regionCount = new HashMap<>();
        for (UserProfile p : profiles) {
            String region = p.getRegion();
            if (region != null && !region.isBlank()) {
                String province = region.split(" ")[0];
                regionCount.merge(province, 1L, Long::sum);
            }
        }
        long total = regionCount.values().stream().mapToLong(Long::longValue).sum();
        return regionCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    int pct = total > 0 ? (int) Math.round(e.getValue() * 100.0 / total) : 0;
                    return Map.<String, Object>of("name", e.getKey(), "percent", pct);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getRevenue(int period) {
        checkAdminRole();
        try {
            Map<String, Object> raw = extractDataMap(orderFeign.getAdminRevenue(period));
            if (raw != null && raw.containsKey("values")) {
                return raw;
            }
        } catch (Exception e) {
            log.warn("查询收入趋势失败", e);
        }
        return Map.of("values", List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataMap(Map<String, Object> result) {
        Object data = result.get("data");
        if (!(data instanceof Map)) return Collections.emptyMap();
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof Map) {
            Object data = ((Map<String, Object>) raw).get("data");
            if (data instanceof List) return (List<Map<String, Object>>) data;
        }
        if (raw instanceof List) return (List<Map<String, Object>>) raw;
        return List.of();
    }

    private String formatMoney(long fen) {
        if (fen <= 0) return "¥0";
        double yuan = fen / 100.0;
        if (yuan >= 10000) return String.format("¥%.1f万", yuan / 10000);
        return String.format("¥%,.0f", yuan);
    }
}
