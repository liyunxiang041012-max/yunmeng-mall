package com.liyun.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.pay.domain.dto.CartDTO;
import com.liyun.pay.domain.pojo.Cart;
import com.liyun.pay.domain.vo.CartVO;

import java.util.List;

/**
 * <p>
 * 购物车表 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-21
 */
public interface ICartService extends IService<Cart> {

    void addCart(CartDTO dto);

    List<CartVO> cartList();

    void updateCart(CartDTO dto);

    void deleteCart(List<Long> ids);

    /**
     * 根据skuId列表删除购物车商品
     */
    void deleteCartBySkuIds(List<Long> skuIds);
}
