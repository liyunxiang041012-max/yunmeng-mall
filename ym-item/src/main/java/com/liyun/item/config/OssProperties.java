package com.liyun.item.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性
 * 对应 Nacos shared-oss.yaml:
 * <pre>
 * aliyun:
 *   oss:
 *     endpoint: oss-cn-hangzhou.aliyuncs.com
 *     dir-prefix:
 *       item: item
 *       user-header: user-header
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /** OSS Endpoint */
    private String endpoint;

    /** 访问密钥ID */
    private String accessKeyId;

    /** 访问密钥Secret */
    private String accessKeySecret;

    /** Bucket名称 */
    private String bucketName;

    /** CDN加速域名 (可选) */
    private String cdnDomain;

    /** 目录前缀 */
    private DirPrefix dirPrefix = new DirPrefix();

    @Data
    public static class DirPrefix {
        /** 商品/店铺图片目录 */
        private String item = "item";
        /** 用户头像目录 */
        private String userHeader = "user-header";
    }
}
