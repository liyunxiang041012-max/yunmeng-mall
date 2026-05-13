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
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    // 搜索页已移出白名单，需要登录才能访问
    private static final List<String> WHITE_LIST = List.of(
            "/us/user/register",
            "/us/user/login",
            "/us/user/sendCode"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ReactiveStringRedisTemplate redisTemplate;

    // Redis key 前缀，与登录服务保持一致
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

        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            return unauthorized(exchange);
        }

        // 先用 JWT 解析，拿到 userId
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

        // 再去 Redis 验证 token 是否存在（响应式链式调用）
        String finalToken = token;
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(savedToken -> {
                    // Redis 里的 token 和请求携带的 token 必须一致
                    if (!savedToken.equals(finalToken)) {
                        log.warn("token 已失效或已在其他设备登录，userId={}", userId);
                        return unauthorized(exchange);
                    }
                    // 验证通过，写入下游请求头
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("user-id", String.valueOf(userId))
                            .header("user-role", String.valueOf(role))
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Redis 中不存在，说明未登录或已被踢下线
                    log.debug("Redis 中无此 token，userId={}", userId);
                    return unauthorized(exchange);
                }));
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
        return -100;
    }
}