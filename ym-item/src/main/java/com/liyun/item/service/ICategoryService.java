package com.liyun.item.service;

import com.liyun.item.domain.dto.CategorySaveDTO;
import com.liyun.item.domain.pojo.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 商品分类 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
public interface ICategoryService extends IService<Category> {

    /** 管理员 - 获取全部未删除分类（平铺列表） */
    List<Category> listAll();

    /** 管理员 - 新增分类 */
    Category saveCategory(CategorySaveDTO dto);

    /** 管理员 - 编辑分类 */
    Category updateCategory(Long id, CategorySaveDTO dto);

    /** 管理员 - 级联软删除分类及其子孙 */
    void deleteCascade(Long id);

    /** 公开 - 查顶级分类 */
    List<Category> listTop();

    /** 公开 - 查子分类 */
    List<Category> listChildren(Long parentId);
}
