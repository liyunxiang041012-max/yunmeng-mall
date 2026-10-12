package com.liyun.item.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.dto.CategorySaveDTO;
import com.liyun.item.domain.pojo.Category;
import com.liyun.item.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员后台 - 分类管理
 */
@RestController
@RequestMapping("/categories/admin")
@Tag(name = "管理员分类管理", description = "管理员对商品分类的增删改查")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final ICategoryService categoryService;

    @Operation(summary = "获取全部分类（平铺列表）")
    @GetMapping("/all")
    public Result<List<Category>> listAll() {
        checkAdminRole();
        return Result.success(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Category> save(@Valid @RequestBody CategorySaveDTO dto) {
        checkAdminRole();
        Category saved = categoryService.saveCategory(dto);
        return Result.success(saved);
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        checkAdminRole();
        Category updated = categoryService.updateCategory(id, dto);
        return Result.success(updated);
    }

    @Operation(summary = "删除分类（级联软删除子分类）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdminRole();
        categoryService.deleteCascade(id);
        return Result.success();
    }

    private void checkAdminRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new RuntimeException("仅管理员可访问此接口");
        }
    }
}
