package com.liyun.item.query;

import com.liyun.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ItemPageQuery extends PageQuery {

    private String keyword;

    private Long categoryId;

    private Long brandId;

    private Long minPrice;

    private Long maxPrice;

    private Boolean inStock;
}