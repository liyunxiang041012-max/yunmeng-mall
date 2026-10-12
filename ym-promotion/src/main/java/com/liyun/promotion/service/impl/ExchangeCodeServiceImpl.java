package com.liyun.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.CollUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.promotion.domain.po.Coupon;
import com.liyun.promotion.domain.po.ExchangeCode;
import com.liyun.promotion.domain.vo.ExchangeCodeVO;
import com.liyun.promotion.mapper.ExchangeCodeMapper;
import com.liyun.promotion.query.CodeQuery;
import com.liyun.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyun.promotion.utils.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.liyun.promotion.constants.PromotionConstants.COUPON_CODE_MAP_KEY;
import static com.liyun.promotion.constants.PromotionConstants.COUPON_CODE_SERIAL_KEY;

@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private BoundValueOperations<String, String> serialOps;
    private final StringRedisTemplate redisTemplate;

    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps = redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }

    @Override
    @Transactional
    @Async("generateExchangeCodeExecutor")
    public void asynGenerateCodes(Coupon coupon) {
        Integer totalNum = coupon.getTotalNum();
        // 1.获取redis自增序列号
        Long result = serialOps.increment(totalNum);
        if (result == null) {
            return;
        }
        int maxSerialNum = result.intValue();
        List<ExchangeCode> list = new ArrayList<>(totalNum);
        for (int serialNum = (maxSerialNum - totalNum + 1); serialNum <= maxSerialNum; serialNum++) {
            // 2.生成兑换码
            String s = CodeUtil.generateCode(serialNum, coupon.getId());
            ExchangeCode e = new ExchangeCode();
            e.setId(serialNum);
            e.setCode(s);
            e.setExchangeTargetId(coupon.getId());
            e.setExpiredTime(coupon.getIssueEndTime());
            list.add(e);
        }
        // 3.保存数据库
        saveBatch(list);
    }

    @Override
    public PageDTO<ExchangeCodeVO> pageQueryCode(CodeQuery query) {
        Page<ExchangeCode> page = lambdaQuery()
                .eq(ExchangeCode::getExchangeTargetId, query.getCouponId())
                .eq(query.getStatus() != null, ExchangeCode::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ExchangeCode> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        List<ExchangeCodeVO> vo = BeanUtils.copyList(records, ExchangeCodeVO.class);
        return PageDTO.of(page, vo);
    }

    @Override
    public boolean updateExchangeMark(long serialNum, boolean b) {
        Boolean boo = redisTemplate.opsForValue().setBit(COUPON_CODE_MAP_KEY, serialNum, b);
        return boo != null && boo;
    }
}
