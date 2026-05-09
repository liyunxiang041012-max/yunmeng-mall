package com.liyun.gateway.filter;


import com.liyun.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    // 白名单，不需要登录的接口
    private static final List<String> WHITE_LIST = List.of(
            "/us/user/register",
            "/us/user/login",
            "/us/user/sendCode",
            "/us/home/**"
            );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // 白名单直接放行
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        // 取token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 没token直接401
        if (token == null || token.isEmpty()) {
            return unauthorized(exchange);
        }

        // 解析token
        try {
            Claims claims = JwtUtils.parseToken(token, secret);
            Long userId = claims.get("userId", Long.class);
            Integer role = claims.get("role", Integer.class);

            // 写入请求头，转发给下游
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("user-id", String.valueOf(userId))
                    .header("user-role", String.valueOf(role))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.warn("token解析失败：{}", e.getMessage());
            return unauthorized(exchange);
        }
    }

    private boolean isWhitePath(String path) {
        return WHITE_LIST.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // 最高优先级
    }
}