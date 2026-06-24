package com.liyun.promotion.service;

import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.po.Coupon;
import com.liyun.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.promotion.domain.vo.ExchangeCodeVO;
import com.liyun.promotion.query.CodeQuery;

public interface IExchangeCodeService extends IService<ExchangeCode> {

    void asynGenerateCodes(Coupon coupon);

    PageDTO<ExchangeCodeVO> pageQueryCode(CodeQuery query);

    boolean updateExchangeMark(long serialNum, boolean b);
}
