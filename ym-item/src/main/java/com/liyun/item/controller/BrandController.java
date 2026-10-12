package com.liyun.item.controller;

import com.liyun.common.utils.Result;
import com.liyun.item.domain.pojo.Brand;
import com.liyun.item.service.IBrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/brands")
@Tag(name = "品牌查询", description = "品牌公开查询接口")
@RequiredArgsConstructor
public class BrandController {

    private final IBrandService brandService;

    @Operation(summary = "查询全部品牌列表")
    @GetMapping("/list")
    public Result<List<Brand>> list() {
        return Result.success(brandService.listActive());
    }
}
