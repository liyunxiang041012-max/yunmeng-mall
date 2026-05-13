package com.liyun.item.domain.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ItemDetailVO {
    private Long id;
    private String name;
    private Long price;
    private Long originalPrice;
    private String mainImage;
    private List<String> images;
    private Integer sold;
    private String brandName;
    private String categoryName;
    private String description;
    private String detailImgs;

    private List<SkuVO> skus;
    private List<SpecGroupVO> specs;

    @Data
    public static class SkuVO {
        private Long id;
        private Long itemId;
        private String skuName;
        private Long price;
        private Integer stock;
        private String image;
        private Map<String, String> specData;
    }

    @Data
    public static class SpecGroupVO {
        private String specName;
        private List<SpecValueVO> values;
    }

    @Data
    public static class SpecValueVO {
        private String value;
        private boolean stock;
    }
}