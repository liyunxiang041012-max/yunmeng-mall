package com.liyun.common.interceptor;

import com.liyun.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("user-id");
        String role = request.getHeader("user-role");
        if (userId != null) {
            UserContext.setUserId(Long.parseLong(userId));
        }
        if (role != null) {
            UserContext.setRole(Integer.parseInt(role));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear(); // 防止内存泄漏
    }
}