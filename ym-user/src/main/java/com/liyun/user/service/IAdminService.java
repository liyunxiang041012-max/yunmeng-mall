package com.liyun.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.common.utils.PageDTO;
import com.liyun.user.domain.pojo.User;

import java.util.Map;

/**
 * 管理员用户管理 服务接口
 *
 * @author liyun
 * @since 2026-06-23
 */
public interface IAdminService extends IService<User> {

    /**
     * 分页查询用户列表（含头像）
     */
    PageDTO<Map<String, Object>> pageUsers(Integer page, Integer size, String keyword, Integer role, Integer status);

    /**
     * 查询单个用户详情（含 Profile 扩展字段）
     */
    Map<String, Object> getUserDetail(Long userId);

    /**
     * 启用/禁用用户
     */
    void toggleUserStatus(Long userId, Integer status);

    /**
     * 修改用户角色
     */
    void changeUserRole(Long userId, Integer role);

    /**
     * 管理后台仪表盘概览数据
     */
    Map<String, Object> getOverview();

    /**
     * 管理后台收入趋势
     */
    Map<String, Object> getRevenue(int period);
}
