package com.liyun.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.pay.domain.dto.PayDTO;
import com.liyun.pay.domain.pojo.Pay;
import com.liyun.pay.domain.vo.PayVO;

import java.util.List;

/**
 * <p>
 * 支付表 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-22
 */
public interface IPayService extends IService<Pay> {

    PayVO createPay(PayDTO dto);

    List<PayVO> payList();

    PayVO getPayDetail(Long id);

    void cancelPay(Long id);

    void handlePayCallback(String payNo);
}
