package com.liyun.item;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {
        ReactiveElasticsearchRepositoriesAutoConfiguration.class
})
@MapperScan("com.liyun.item.mapper")
@EnableFeignClients(basePackages = "com.liyun.api.client")
public class ItemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItemApplication.class, args);
    }

}
