package com.liyun.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {

    /** API Key */
    private String apiKey;

    /** API 地址 */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 超时秒数 */
    private int timeout = 30;

    /** 温度 */
    private double temperature = 0.7;

    /** 最大 token */
    private int maxTokens = 1000;
}
