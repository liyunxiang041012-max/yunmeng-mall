package com.liyun.gateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import com.liyun.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    private static final List<String> WHITE_LIST = List.of(
            "/us/user/register",
            "/us/user/login",
            "/us/user/shop/login",
            "/us/user/admin/login",
            "/us/user/sendCode",
            "/it/shop/register"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String TOKEN_KEY_PREFIX = "user:token:";

    @PostConstruct
    public void testRedis() {
        redisTemplate.opsForValue().get("user:token:1")
                .subscribe(
                        v -> log.info("测试Redis读取成功: {}", v),
                        e -> log.error("Redis连接异常: {}", e.getMessage()),
                        () -> log.warn("测试Redis读取结果为空(empty)")
                );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // 1. 白名单直接放行
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取并处理 Token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            return unauthorized(exchange);
        }

        // 3. 解析 JWT
        Claims claims;
        try {
            claims = JwtUtils.parseToken(token, secret);
        } catch (Exception e) {
            log.warn("token解析失败：{}", e.getMessage());
            return unauthorized(exchange);
        }

        Long userId = claims.get("userId", Long.class);
        Integer role = claims.get("role", Integer.class);
        String redisKey = TOKEN_KEY_PREFIX + userId;

        String finalToken = token;

        // 👇 4. 核心修复：先校验 Token，返回 Boolean，避免 Mono<Void> 陷阱
        Mono<Boolean> checkToken = redisTemplate.opsForValue().get(redisKey)
                .map(savedToken -> {
                    boolean isValid = savedToken.equals(finalToken);
                    if (!isValid) {
                        log.warn("token 已失效或已在其他设备登录，userId={}", userId);
                    }
                    return isValid;
                })
                .defaultIfEmpty(false); // 如果 Redis 没查到，默认返回 false

        // 👇 5. 根据校验结果，决定是放行还是拦截
        return checkToken.flatMap(isValid -> {
            if (!isValid) {
                log.debug("Redis 中无此 token 或校验失败，userId={}", userId);
                return unauthorized(exchange);
            }

            // 验证通过，写入下游请求头
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("user-id", String.valueOf(userId))
                    .header("user-role", String.valueOf(role))
                    // 获取真实 IP，兼容 IPv6 和未获取到的情况
                    .header("X-Real-IP", exchange.getRequest().getRemoteAddress() != null ?
                            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown")
                    .build();

            // 放行请求
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        });
    }

    private boolean isWhitePath(String path) {
        return WHITE_LIST.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"未登录或Token过期\",\"timestamp\":" + System.currentTimeMillis() + "}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
