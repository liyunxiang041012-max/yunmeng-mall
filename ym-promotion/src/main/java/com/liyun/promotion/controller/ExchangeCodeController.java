package com.liyun.promotion.controller;

import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.promotion.domain.vo.ExchangeCodeVO;
import com.liyun.promotion.query.CodeQuery;
import com.liyun.promotion.service.IExchangeCodeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/codes")
@RequiredArgsConstructor
public class ExchangeCodeController {

    private final IExchangeCodeService codeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询兑换码")
    public Result<PageDTO<ExchangeCodeVO>> pageQueryCode(CodeQuery query) {
        return Result.success(codeService.pageQueryCode(query));
    }
}
