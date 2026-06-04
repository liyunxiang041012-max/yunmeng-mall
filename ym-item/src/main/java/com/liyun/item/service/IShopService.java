package com.liyun.item.service;

import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.pojo.Shop;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.item.domain.vo.ShopCartVO;
import com.liyun.item.domain.vo.ShopDetailVO;
import com.liyun.item.domain.vo.ItemVO;

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

    /**
     * 获取店铺详情（包含统计信息）
     * @param shopId 店铺ID
     * @return 店铺详情VO
     */
    ShopDetailVO getShopDetail(Long shopId);

    /**
     * 获取店铺商品列表（分页）
     * @param shopId 店铺ID
     * @param sort 排序方式：default(综合) / sales(销量) / price(价格) / newest(最新)
     * @param page 页码
     * @param size 每页条数
     * @return 商品分页列表
     */
    PageDTO<ItemVO> getShopItems(Long shopId, String sort, Integer page, Integer size);
}
