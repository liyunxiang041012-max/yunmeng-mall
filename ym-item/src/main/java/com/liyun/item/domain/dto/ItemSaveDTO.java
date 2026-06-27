package com.liyun.item.domain.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 商家保存/编辑商品请求
 */
@Data
public class ItemSaveDTO {
    private String name;
    private String image;
    private Long categoryId;
    private Long brandId;
    private Long price;
    private Integer stock;

    /** 规格组（多规格时传） */
    private List<SpecDTO> specs;
    /** SKU明细（多规格时传） */
    private List<SkuDTO> skus;

    @Data
    public static class SpecDTO {
        private String specName;
        private List<String> values;
    }

    @Data
    public static class SkuDTO {
        private Map<String, String> specData;
        private Long price;
        private Integer stock;
        private String image;
    }
}
