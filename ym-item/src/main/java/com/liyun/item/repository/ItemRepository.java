package com.liyun.item.repository;

import com.liyun.item.domain.doc.ItemDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ItemRepository extends ElasticsearchRepository<ItemDoc, Long> {
}