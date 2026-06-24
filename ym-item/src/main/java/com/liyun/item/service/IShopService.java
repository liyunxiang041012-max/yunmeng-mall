package com.liyun.item.service;

import com.liyun.api.dto.ShopInfoDTO;
import com.liyun.common.utils.PageDTO;
import com.liyun.item.domain.dto.ShopDTO;
import com.liyun.item.domain.dto.ShopEntryDTO;
import com.liyun.item.domain.dto.ShopSetupDTO;
import com.liyun.item.domain.pojo.Shop;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.item.domain.vo.ShopCartVO;
import com.liyun.item.domain.vo.ShopEntryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    void setupShop(Long userId, ShopSetupDTO dto);

    Map<String, Object> getShopStatus();

    ShopCartVO getCartShopInfo(Long id);

    List<ShopCartVO> batchGetCartShopInfo(List<Long> shopIds);

    ShopInfoDTO getShopInfo(Long shopId);

    List<ShopInfoDTO> batchGetShopInfo(List<Long> shopIds);

    String uploadShopAvatar(MultipartFile file);

    ShopEntryVO entryShop(Long userId, ShopEntryDTO dto);

    /** ======== 商家仪表盘 ======== */

    Shop getShopById(Long shopId);

    Map<String, Object> getShopStats();

    /** ======== 管理后台 CRUD ======== */

    PageDTO<Shop> listShops(Integer page, Integer size, String keyword, Integer status);

    void updateShopInfo(Long shopId, String shopName, String logo, String description);

    void deleteShop(Long shopId);

    void toggleShopStatus(Long shopId);
}
