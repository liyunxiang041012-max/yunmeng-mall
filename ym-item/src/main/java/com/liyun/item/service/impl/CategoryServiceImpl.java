package com.liyun.item.service.impl;

import com.liyun.item.domain.pojo.Category;
import com.liyun.item.mapper.CategoryMapper;
import com.liyun.item.service.ICategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品分类 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

}
