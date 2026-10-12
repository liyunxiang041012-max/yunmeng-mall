package com.liyun.item.domain.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "item")
public class ItemDoc {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;

    @Field(type = FieldType.Keyword)
    private String image;

    @Field(type = FieldType.Long)
    private Long price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Integer)
    private Integer sold;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Long)
    private Long shopId;

    @Field(type = FieldType.Keyword)
    private String shopName;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Integer)
    private Integer auditStatus;
}