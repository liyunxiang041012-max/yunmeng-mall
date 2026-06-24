package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.item.domain.dto.CategorySaveDTO;
import com.liyun.item.domain.pojo.Category;
import com.liyun.item.mapper.CategoryMapper;
import com.liyun.item.service.ICategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 商品分类 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Override
    public List<Category> listAll() {
        return lambdaQuery()
                .eq(Category::getDeleted, 0)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId)
                .list();
    }

    @Override
    public List<Category> listTop() {
        return lambdaQuery()
                .eq(Category::getParentId, 0)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .list();
    }

    @Override
    public List<Category> listChildren(Long parentId) {
        return lambdaQuery()
                .eq(Category::getParentId, parentId)
                .eq(Category::getStatus, 1)
                .eq(Category::getDeleted, 0)
                .orderByAsc(Category::getSort)
                .list();
    }

    @Override
    @Transactional
    public Category saveCategory(CategorySaveDTO dto) {
        // 校验
        validateName(dto.getName());
        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;
        if (parentId != 0 && getById(parentId) == null) {
            throw new RuntimeException("父分类不存在");
        }
        checkNameDuplicate(dto.getName(), parentId, null);

        Category category = new Category();
        category.setName(dto.getName());
        category.setParentId(parentId);
        category.setLevel(calcLevel(parentId));
        category.setSort(0);
        category.setStatus(1);
        category.setDeleted(0);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        save(category);
        return category;
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, CategorySaveDTO dto) {
        Category category = getById(id);
        if (category == null || category.getDeleted() == 1) {
            throw new RuntimeException("分类不存在");
        }

        validateName(dto.getName());
        Long parentId = dto.getParentId() != null ? dto.getParentId() : category.getParentId();

        // 不能把自己或子孙设为父分类
        if (parentId != 0) {
            if (parentId.equals(id)) {
                throw new RuntimeException("不能将分类的父分类设为自己");
            }
            if (getById(parentId) == null) {
                throw new RuntimeException("父分类不存在");
            }
            if (isDescendantOf(parentId, id)) {
                throw new RuntimeException("不能将分类移到其子孙分类下");
            }
        }

        checkNameDuplicate(dto.getName(), parentId, id);

        category.setName(dto.getName());
        category.setParentId(parentId);
        category.setLevel(calcLevel(parentId));
        category.setUpdateTime(LocalDateTime.now());
        updateById(category);
        return category;
    }

    @Override
    @Transactional
    public void deleteCascade(Long id) {
        Category category = getById(id);
        if (category == null || category.getDeleted() == 1) {
            throw new RuntimeException("分类不存在");
        }

        // 收集所有子孙ID
        List<Long> allIds = new ArrayList<>();
        allIds.add(id);
        collectChildren(id, allIds);

        // 批量软删除
        List<Category> updates = allIds.stream().map(cid -> {
            Category c = new Category();
            c.setId(cid);
            c.setDeleted(1);
            c.setUpdateTime(LocalDateTime.now());
            return c;
        }).collect(Collectors.toList());
        updateBatchById(updates);
    }

    // ==================== 私有方法 ====================

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("分类名称不能为空");
        }
        if (name.length() < 2 || name.length() > 50) {
            throw new RuntimeException("分类名称长度需在2-50字符之间");
        }
    }

    private void checkNameDuplicate(String name, Long parentId, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name)
                .eq(Category::getParentId, parentId)
                .eq(Category::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new RuntimeException("该分类名称已存在");
        }
    }

    /** 计算层级 */
    private Integer calcLevel(Long parentId) {
        if (parentId == 0) return 1;
        Category parent = getById(parentId);
        return parent != null ? parent.getLevel() + 1 : 1;
    }

    /** 递归收集所有子孙分类ID */
    private void collectChildren(Long parentId, List<Long> result) {
        List<Category> children = lambdaQuery()
                .eq(Category::getParentId, parentId)
                .eq(Category::getDeleted, 0)
                .list();
        for (Category child : children) {
            result.add(child.getId());
            collectChildren(child.getId(), result);
        }
    }

    /** 判断 candidateParent 是否是 targetId 的子孙 */
    private boolean isDescendantOf(Long candidateParent, Long targetId) {
        List<Long> descendants = new ArrayList<>();
        collectChildren(targetId, descendants);
        return descendants.contains(candidateParent);
    }
}
