package com.liyun.promotion.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.promotion.domain.po.FlashSaleItem;
import com.liyun.promotion.service.IFlashSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动 - 商家端控制器
 * 商家直接提交已审核通过的商品参与秒杀，无需选择活动（平台常驻活动自动关联）
 * 折扣必须 >= 20%（至少8折），商家自定秒杀数量、时间
 * 一旦发布参与秒杀，无法撤回/结束
 * 仅未开始的秒杀商品可调整时间
 */
@Slf4j
@RestController
@RequestMapping("/merchant/flash-sales")
@RequiredArgsConstructor
@Tag(name = "秒杀活动（商家端）", description = "商家参与秒杀活动管理")
public class FlashSaleMerchantController {

    private final IFlashSaleService flashSaleService;

    @PostMapping("/items")
    @Operation(summary = "商家提交商品参与秒杀（自动关联平台活动，折扣>=20%，发布后不可撤回）")
    public Result<Void> submitItems(@RequestBody List<FlashSaleItem> items) {
        checkMerchantRole();
        Long userId = UserContext.getUserId();
        log.info("[FLASH-MERCHANT] 商家userId={} 提交{}件商品参与秒杀", userId, items.size());
        flashSaleService.merchantSubmitItems(items);
        return Result.success();
    }

    @PutMapping("/items/{itemId}/time")
    @Operation(summary = "调整未开始秒杀商品的时间（仅未开始可调）")
    public Result<Void> updateTime(
            @PathVariable Long itemId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        checkMerchantRole();
        flashSaleService.merchantUpdateTime(itemId, startTime, endTime);
        return Result.success();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "商家秒杀看板：查看自己所有秒杀商品的销量、价格、状态")
    public Result<List<Map<String, Object>>> dashboard(
            @RequestParam Long shopId) {
        checkMerchantRole();
        return Result.success(flashSaleService.getMerchantDashboard(shopId));
    }

    @GetMapping("/items")
    @Operation(summary = "商家分页查询自己的秒杀商品", description = "支持按状态筛选：warming/ongoing/ended，不传则查全部")
    public Result<PageDTO<Map<String, Object>>> queryFlashSaleItems(
            @RequestParam Long shopId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        checkMerchantRole();
        return Result.success(flashSaleService.merchantQueryFlashSaleItems(shopId, pageNo, pageSize, status));
    }

    private void checkMerchantRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new RuntimeException("仅商家可访问此接口");
        }
    }
}
