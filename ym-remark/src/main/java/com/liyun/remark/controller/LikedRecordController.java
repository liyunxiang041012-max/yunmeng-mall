package com.liyun.remark.controller;

import com.liyun.common.utils.Result;
import com.liyun.remark.domain.dto.LikeRecordFormDTO;
import com.liyun.remark.service.ILikedRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Tag(name = "点赞业务相关接口")
public class LikedRecordController {

    private final ILikedRecordService likedRecordService;

    @PostMapping
    @Operation(summary = "点赞或取消点赞")
    public Result<Void> addLikeRecord(@RequestBody @Valid LikeRecordFormDTO recordFormDTO) {
        likedRecordService.addLikeRecord(recordFormDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "查询指定业务id的点赞状态（兼容逗号分隔和数组格式）")
    public Result<Set<Long>> isBizLiked(@RequestParam(value = "bizIds", required = false) String bizIdsStr,
                                         @RequestParam(value = "bizIds[]", required = false) List<Long> bizIdsArr) {
        List<Long> bizIds;
        if (bizIdsArr != null && !bizIdsArr.isEmpty()) {
            bizIds = bizIdsArr;
        } else if (bizIdsStr != null && !bizIdsStr.isEmpty()) {
            bizIds = Arrays.stream(bizIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } else {
            bizIds = List.of();
        }
        return Result.success(likedRecordService.isBizLiked(bizIds));
    }
}
