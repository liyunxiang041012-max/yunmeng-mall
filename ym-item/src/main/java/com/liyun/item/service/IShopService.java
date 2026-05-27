package com.liyun.item.service;

import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.pojo.Shop;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.item.domain.vo.ShopCartVO;

import java.util.List;

/**
 * <p>
 * 商家 服务类
 * </p>
 *
 * @author liyun
 * @since 2026-05-09
 */
public interface IShopService extends IService<Shop> {

    void addShop(ShopDTO dto, String ip);

    ShopCartVO getCartShopInfo(Long id);

    List<ShopCartVO> batchGetCartShopInfo(List<Long> shopIds);

    ShopInfoDTO getShopInfo(Long shopId);

    List<ShopInfoDTO> batchGetShopInfo(List<Long> shopIds);
}
