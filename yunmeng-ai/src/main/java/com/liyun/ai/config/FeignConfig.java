package com.liyun.ai.config;

import com.liyun.common.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 方式1：从 UserContext（ThreadLocal）直接获取，更可靠（同线程调用）
                Long userId = UserContext.getUserId();
                if (userId != null) {
                    template.header("user-id", String.valueOf(userId));
                }
                Integer role = UserContext.getRole();
                if (role != null) {
                    template.header("user-role", String.valueOf(role));
                }

                // 方式2：从原始请求透传 Authorization（RequestContextHolder 在同线程同步调用下可用）
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        template.header("Authorization", authHeader);
                    }
                }
            }
        };
    }
}
