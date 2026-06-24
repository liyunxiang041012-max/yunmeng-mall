package com.liyun.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.user.enums.GenderEnum;
import com.liyun.user.domain.pojo.User;
import com.liyun.user.domain.pojo.UserProfile;
import com.liyun.user.mapper.UserMapper;
import com.liyun.user.service.IAdminService;
import com.liyun.user.service.IUserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        // 性别分布（从 user_profile 统计）
        long maleCount = userProfileService.count(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getGender, GenderEnum.MALE.getCode()));
        long femaleCount = userProfileService.count(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getGender, GenderEnum.FEMALE.getCode()));
        long totalGender = maleCount + femaleCount;
        int malePercent = totalGender > 0 ? (int) Math.round(maleCount * 100.0 / totalGender) : 58;
        int femalePercent = totalGender > 0 ? (int) Math.round(femaleCount * 100.0 / totalGender) : 42;

        // 地域分布（从 user_profile.region 统计 TOP5）
        List<Map<String, Object>> regions = computeRegionTop5();

        // ===== 组装 stats =====
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("gmv",        Map.of("value", "¥—",     "trend", 0.0, "delta", "—"));
        stats.put("orders",     Map.of("value", "—",      "trend", 0.0, "delta", "—"));
        stats.put("users",      Map.of("value", String.valueOf(totalUsers), "trend", 0.0, "delta", String.valueOf(activeUsers)));
        stats.put("conversion", Map.of("value", "—",      "trend", 0.0, "delta", "—"));

        // ===== 组装 userPortrait =====
        Map<String, Object> userPortrait = new LinkedHashMap<>();
        userPortrait.put("gender", Map.of("male", malePercent, "female", femalePercent));
        userPortrait.put("ages", List.of(
                Map.of("label", "18-24", "percent", 28, "color", "#D9A53C"),
                Map.of("label", "25-34", "percent", 42, "color", "#1A1712"),
                Map.of("label", "35-44", "percent", 18, "color", "#8C6308"),
                Map.of("label", "45-54", "percent", 8,  "color", "#9B9484"),
                Map.of("label", "55+",   "percent", 4,  "color", "#C0B8A8")
        ));
        userPortrait.put("regions", regions);

        // ===== 组装结果 =====
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("userPortrait", userPortrait);
        result.put("consumeLevels", List.of(
                Map.of("label", "高消费",   "percent", 55, "color", "#D9A53C"),
                Map.of("label", "中等消费", "percent", 25, "color", "#1A1712"),
                Map.of("label", "低消费",   "percent", 20, "color", "#8C6308")
        ));
        result.put("recentOrders", List.of(
                Map.of("id", 1, "orderNo", "—", "user", "—", "product", "—",
                       "amount", "—", "status", "—", "statusClass", "pending", "time", "—"),
                Map.of("id", 2, "orderNo", "—", "user", "—", "product", "—",
                       "amount", "—", "status", "—", "statusClass", "pending", "time", "—"),
                Map.of("id", 3, "orderNo", "—", "user", "—", "product", "—",
                       "amount", "—", "status", "—", "statusClass", "pending", "time", "—"),
                Map.of("id", 4, "orderNo", "—", "user", "—", "product", "—",
                       "amount", "—", "status", "—", "statusClass", "pending", "time", "—"),
                Map.of("id", 5, "orderNo", "—", "user", "—", "product", "—",
                       "amount", "—", "status", "—", "statusClass", "pending", "time", "—")
        ));
        result.put("topProducts", List.of(
                Map.of("name", "—", "sales", "—", "revenue", "—", "color", "#FDE8C8"),
                Map.of("name", "—", "sales", "—", "revenue", "—", "color", "#DBEAFE"),
                Map.of("name", "—", "sales", "—", "revenue", "—", "color", "#E8D5F5"),
                Map.of("name", "—", "sales", "—", "revenue", "—", "color", "#D5F0E2"),
                Map.of("name", "—", "sales", "—", "revenue", "—", "color", "#FCE7F3")
        ));
        return result;
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
        int count = switch (period) {
            case 7 -> 7;
            case 30 -> 30;
            case 90 -> 12;
            default -> 7;
        };
        List<Integer> values = new ArrayList<>();
        Random rnd = new Random(42);
        for (int i = 0; i < count; i++) {
            values.add(3000 + rnd.nextInt(12000));
        }
        return Map.of("values", values);
    }
}
