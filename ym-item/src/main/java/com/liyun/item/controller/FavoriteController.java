package com.liyun.item.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.vo.FavoriteItemVO;
import com.liyun.item.service.IFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商品收藏 前端控制器
 * </p>
 *
 * @author liyun
 * @since 2026-05-28
 */
@RestController
@RequestMapping("/favorites")
@Tag(name = "商品收藏", description = "商品收藏接口")
@RequiredArgsConstructor
public class FavoriteController {

    private final IFavoriteService favoriteService;

    @Operation(summary = "切换收藏状态")
    @PostMapping("/toggle/{itemId}")
    public Result<Boolean> toggleFavorite(@PathVariable Long itemId) {
        Long userId = UserContext.getUserId();
        boolean favorited = favoriteService.toggleFavorite(userId, itemId);
        return Result.success(favorited);
    }

    @Operation(summary = "检查收藏状态")
    @GetMapping("/check/{itemId}")
    public Result<Boolean> checkFavorite(@PathVariable Long itemId) {
        Long userId = UserContext.getUserId();
        boolean favorited = favoriteService.checkFavorite(userId, itemId);
        return Result.success(favorited);
    }

    @Operation(summary = "获取我的收藏列表")
    @GetMapping("/my")
    public Result<PageDTO<FavoriteItemVO>> getMyFavorites(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(favoriteService.getMyFavorites(userId, page, size));
    }
}
