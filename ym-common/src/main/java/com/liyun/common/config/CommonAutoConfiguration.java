package com.liyun.common.config;

import com.liyun.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * ym-common 自动配置 —— 确保 GlobalExceptionHandler 等公共组件被依赖方自动扫描
 */
@Configuration
@Import({GlobalExceptionHandler.class})
public class CommonAutoConfiguration {
}
