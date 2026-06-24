package com.liyun.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.item.domain.pojo.Brand;
import com.liyun.item.mapper.BrandMapper;
import com.liyun.item.service.IBrandService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements IBrandService {

    @Override
    public List<Brand> listActive() {
        return list(new LambdaQueryWrapper<Brand>()
                .eq(Brand::getStatus, 1)
                .eq(Brand::getDeleted, 0)
                .orderByAsc(Brand::getId));
    }
}
