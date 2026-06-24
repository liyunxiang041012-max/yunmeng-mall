package com.liyun.item.controller;


import com.liyun.common.utils.Result;
import com.liyun.item.domain.pojo.Category;
import com.liyun.item.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final ICategoryService categoryService;

    @Operation(summary = "查询顶级分类列表")
    @GetMapping("/top")
    public Result<List<Category>> topList() {
        return Result.success(categoryService.listTop());
    }

    @Operation(summary = "查询子分类列表")
    @GetMapping("/children")
    public Result<List<Category>> children(@RequestParam Long parentId) {
        return Result.success(categoryService.listChildren(parentId));
    }
}
