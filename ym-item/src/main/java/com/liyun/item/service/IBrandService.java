package com.liyun.item.service;

import com.liyun.item.domain.pojo.Brand;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 品牌 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
public interface IBrandService extends IService<Brand> {

    /** 查询全部启用品牌 */
    List<Brand> listActive();
}
