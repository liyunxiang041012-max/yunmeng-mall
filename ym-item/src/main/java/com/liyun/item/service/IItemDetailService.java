package com.liyun.item.service;

import com.liyun.item.domain.pojo.ItemDetail;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.item.domain.vo.ItemDetailVO;

/**
 * <p>
 * 商品详情 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
public interface IItemDetailService extends IService<ItemDetail> {

    ItemDetailVO getItemDetail(Long itemId);
}
