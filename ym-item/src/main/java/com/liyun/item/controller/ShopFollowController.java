package com.liyun.item.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.vo.FollowShopVO;
import com.liyun.item.service.IShopFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/follow")
@Tag(name = "店铺关注管理", description = "用户关注/取消关注店铺接口")
@RequiredArgsConstructor
public class ShopFollowController {

    private final IShopFollowService shopFollowService;

    @Operation(summary = "切换关注状态")
    @PostMapping("/toggle/{shopId}")
    public Result<Boolean> toggleFollow(@PathVariable Long shopId) {
        Long userId = UserContext.getUserId();
        boolean followed = shopFollowService.toggleFollow(userId, shopId);
        return Result.success(followed);
    }

    @Operation(summary = "检查是否已关注")
    @GetMapping("/check/{shopId}")
    public Result<Boolean> checkFollow(@PathVariable Long shopId) {
        Long userId = UserContext.getUserId();
        boolean followed = shopFollowService.checkFollow(userId, shopId);
        return Result.success(followed);
    }

    @Operation(summary = "获取店铺粉丝数")
    @GetMapping("/fans-count/{shopId}")
    public Result<Long> countFans(@PathVariable Long shopId) {
        Long count = shopFollowService.countFans(shopId);
        return Result.success(count);
    }

    @Operation(summary = "获取我的关注列表（分页）")
    @GetMapping("/my")
    public Result<PageDTO<FollowShopVO>> getMyFollows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.success(shopFollowService.pageMyFollows(userId, page, size));
    }
}
