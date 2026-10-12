package com.liyun.item.controller;

import com.liyun.common.context.UserContext;
import com.liyun.common.utils.BeanUtils;
import com.liyun.common.utils.PageDTO;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.dto.ItemSaveDTO;
import com.liyun.item.domain.pojo.Item;
import com.liyun.item.service.IItemService;
import com.liyun.item.service.impl.OssUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 商家端 - 商品管理（商家管理自己的商品）
 */
@RestController
@RequestMapping("/shop/item")
@Tag(name = "商家商品管理", description = "商家对自己店铺商品的增删改查")
@RequiredArgsConstructor
public class MerchantItemController {

    private final IItemService itemService;
    private final OssUploadService ossUploadService;

    @Operation(summary = "分页查询我的商品列表")
    @GetMapping("/page")
    public Result<PageDTO<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(itemService.listMyItems(page, size, status, keyword));
    }

    @Operation(summary = "查看单个商品详情")
    @GetMapping("/{itemId}")
    public Result<Item> detail(@PathVariable Long itemId) {
        Item item = itemService.getById(itemId);
        if (item == null) return Result.fail("商品不存在");
        return Result.success(item);
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<Item> save(@RequestBody ItemSaveDTO dto) {
        Item item = BeanUtils.copyBean(dto, Item.class);
        item.setPrice(null);
        item.setStock(null);

        boolean isMultiSpec = dto.getSpecs() != null && !dto.getSpecs().isEmpty();
        Item saved;
        if (isMultiSpec) {
            List<Map<String, Object>> skuList = dto.getSkus().stream()
                    .map(s -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("specData", s.getSpecData());
                        m.put("price", s.getPrice());
                        m.put("stock", s.getStock());
                        m.put("image", s.getImage());
                        return m;
                    }).collect(java.util.stream.Collectors.toList());
            List<String> specNames = dto.getSpecs().stream()
                    .map(ItemSaveDTO.SpecDTO::getSpecName).collect(java.util.stream.Collectors.toList());
            saved = itemService.saveItem(item, specNames, skuList);
        } else {
            item.setPrice(dto.getPrice());
            item.setStock(dto.getStock());
            saved = itemService.saveItem(item);
        }
        return Result.success(saved);
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{itemId}")
    public Result update(@PathVariable Long itemId, @RequestBody ItemSaveDTO dto) {
        Item item = BeanUtils.copyBean(dto, Item.class);
        item.setPrice(null);
        item.setStock(null);

        boolean isMultiSpec = dto.getSpecs() != null && !dto.getSpecs().isEmpty();
        if (isMultiSpec) {
            List<Map<String, Object>> skuList = dto.getSkus().stream()
                    .map(s -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("specData", s.getSpecData());
                        m.put("price", s.getPrice());
                        m.put("stock", s.getStock());
                        m.put("image", s.getImage());
                        return m;
                    }).collect(java.util.stream.Collectors.toList());
            List<String> specNames = dto.getSpecs().stream()
                    .map(ItemSaveDTO.SpecDTO::getSpecName).collect(java.util.stream.Collectors.toList());
            itemService.updateItem(itemId, item, specNames, skuList);
        } else {
            item.setPrice(dto.getPrice());
            item.setStock(dto.getStock());
            itemService.updateItem(itemId, item);
        }
        return Result.success();
    }

    @Operation(summary = "删除商品（软删除）")
    @DeleteMapping("/{itemId}")
    public Result delete(@PathVariable Long itemId) {
        itemService.deleteItem(itemId);
        return Result.success();
    }

    @Operation(summary = "上架/下架商品")
    @PutMapping("/toggle-status/{itemId}")
    public Result<Item> toggleStatus(@PathVariable Long itemId) {
        checkShopRole();
        itemService.toggleItemStatus(itemId);
        return Result.success(itemService.getById(itemId));
    }

    @Operation(summary = "上传商品图片到OSS")
    @PostMapping("/upload/image")
    public Result<String> uploadItemImage(@RequestParam("file") MultipartFile file) {
        checkShopRole();
        String url = ossUploadService.uploadImage(file, "item");
        return Result.success(url);
    }

    /**
     * 校验当前用户是否为商家角色
     */
    private void checkShopRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new RuntimeException("仅商家可访问此接口");
        }
    }
}
