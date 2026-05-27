package com.liyun.common.config;

import com.liyun.common.utils.OssUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置类
 *
 * @author yunmeng
 * @date 2026-05-27
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    /**
     * OSS访问域名
     * 例如: oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * 访问密钥ID
     */
    private String accessKeyId;

    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;

    /**
     * 存储空间名称
     */
    private String bucketName;

    /**
     * 创建OSS工具类Bean
     *
     * @return OssUtils实例
     */
    @Bean
    public OssUtils ossUtils() {
        return new OssUtils(endpoint, accessKeyId, accessKeySecret, bucketName);
    }
}
