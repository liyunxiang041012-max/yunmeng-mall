package com.liyun.ai.controller;

import com.liyun.ai.model.ItemReviewRequest;
import com.liyun.ai.model.ItemReviewResponse;
import com.liyun.ai.service.AiReviewService;
import com.liyun.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/review")
@Tag(name = "AI 商品审核", description = "AI 辅助审核商家提交的商品")
@RequiredArgsConstructor
public class AiReviewController {

    private final AiReviewService aiReviewService;

    @Operation(summary = "AI 审核商品")
    @PostMapping("/item")
    public Result<ItemReviewResponse> reviewItem(@RequestBody ItemReviewRequest request) {
        ItemReviewResponse result = aiReviewService.review(request);
        return Result.success(result);
    }
}
