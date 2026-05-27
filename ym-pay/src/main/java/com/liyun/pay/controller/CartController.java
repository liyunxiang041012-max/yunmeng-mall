package com.liyun.pay.controller;

import com.liyun.pay.domain.dto.CartDTO;
import com.liyun.pay.domain.vo.CartVO;
import com.liyun.pay.service.ICartService;
import com.liyun.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@Tag(name = "购物车管理", description = "购物车增删改查")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @Operation(summary = "添加商品到购物车")
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CartDTO dto) {
        cartService.addCart(dto);
        return Result.success();
    }

    @Operation(summary = "查询购物车列表（批量查询优化）")
    @GetMapping("/list")
    public Result<List<CartVO>> list() {
        List<CartVO> list = cartService.cartList();
        return Result.success(list);
    }

    @Operation(summary = "更新购物车商品数量")
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody CartDTO dto) {
        cartService.updateCart(dto);
        return Result.success();
    }

    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的商品");
        }
        cartService.deleteCart(ids);
        return Result.success();
    }
}
