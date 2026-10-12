package com.liyun.remark.controller;

import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.remark.domain.dto.CommentFormDTO;
import com.liyun.remark.domain.vo.CommentVO;
import com.liyun.remark.service.ICommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "评论相关接口")
public class CommentController {

    private final ICommentService commentService;

    @PostMapping
    @Operation(summary = "发表评论或回复")
    public Result<Void> saveComment(@RequestBody @Valid CommentFormDTO dto) {
        commentService.saveComment(dto);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询评论列表")
    public Result<PageDTO<CommentVO>> pageQueryComments(
            @RequestParam Long bizId,
            @RequestParam String bizType,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(commentService.pageQueryComments(bizId, bizType, pageNo, pageSize));
    }
}
