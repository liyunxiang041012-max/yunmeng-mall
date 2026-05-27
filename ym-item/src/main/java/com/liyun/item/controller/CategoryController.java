package com.liyun.item.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.pojo.Category;
import com.liyun.item.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 商品分类 前端控制器
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final ICategoryService categoryService;
    @Operation(summary = "查询顶级分类列表")
    @GetMapping("/top")
    public Result<List<Category>> topList() {
        return Result.success(categoryService.list(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, 0)
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        ));
    }
}
