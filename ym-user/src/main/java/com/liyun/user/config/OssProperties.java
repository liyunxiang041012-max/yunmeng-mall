package com.liyun.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性
 * 对应 Nacos shared-oss.yaml
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String cdnDomain;

    private DirPrefix dirPrefix = new DirPrefix();

    @Data
    public static class DirPrefix {
        private String item = "item";
        private String userHeader = "user-header";
    }
}
