package com.liyun.pay.controller;

import com.liyun.pay.domain.dto.OrderDTO;
import com.liyun.pay.domain.dto.PayDTO;
import com.liyun.pay.domain.vo.PayVO;
import com.liyun.pay.service.IPayService;
import com.liyun.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pay")
@Tag(name = "支付管理", description = "支付相关接口")
@RequiredArgsConstructor
public class PayController {

    private final IPayService payService;

    @Operation(summary = "创建支付单")
    @PostMapping("/create")
    public Result<PayVO> create(@Valid @RequestBody PayDTO payDTO) {

        return Result.success();

    }

    @Operation(summary = "查询支付记录列表")
    @GetMapping("/list")
    public Result<List<PayVO>> list() {
        List<PayVO> list = payService.payList();
        return Result.success(list);
    }

    @Operation(summary = "查询支付详情")
    @GetMapping("/detail/{id}")
    public Result<PayVO> detail(@PathVariable Long id) {
        PayVO vo = payService.getPayDetail(id);
        return Result.success(vo);
    }

    @Operation(summary = "取消支付")
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        payService.cancelPay(id);
        return Result.success();
    }

    @Operation(summary = "支付成功回调")
    @PostMapping("/callback/{payNo}")
    public Result<Void> payCallback(@PathVariable String payNo) {
        payService.handlePayCallback(payNo);
        return Result.success();
    }
}
