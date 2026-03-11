package com.bit.iot.gateway.filter;

import bit.iot.common.utils.TokenUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Token 验证全局过滤器
 * 
 * @author chenhao
 * @date 2026/3/9
 */
@Component
public class TokenValidationFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange);
        }
        
        // 获取请求路径
        String path = request.getPath().value();
        
        // 放行的路径（不需要鉴权的接口）
        if (isWhitelist(path)) {
            return chain.filter(exchange);
        }
        
        // 从请求头获取 Token
        String token = extractToken(request);
        
        // 验证 Token
        if (!validateToken(token)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "未登录或 Token 已过期");
        }
        
        // 从 Token 中解析用户信息
        String userId = TokenUtil.getUserIdFromToken(token);
        String username = TokenUtil.getUsernameFromToken(token);
        
        if (userId == null || username == null) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Token 无效");
        }
        
        // TODO: 这里可以从缓存或数据库中查询用户的角色和权限
        // 为了演示，暂时不传递角色和权限，由下游服务自行查询
        
        // 将用户信息传递到下游服务
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                // .header("X-User-Roles", String.join(",", roles))  // 需要时打开
                // .header("X-User-Permissions", String.join(",", permissions))  // 需要时打开
                .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
    
    /**
     * 判断是否在白名单中
     */
    private boolean isWhitelist(String path) {
        return path.equals("/user/login") ||           // 登录接口
               path.contains("/user/list") ||             // 错误页面
               path.startsWith("/error") ||             // 错误页面
               path.startsWith("/favicon.ico") ||       // 图标
               path.startsWith("/swagger") ||           // Swagger 文档
               path.startsWith("/v2/api-docs") ||       // Swagger API
               path.startsWith("/v3/api-docs") ||       // OpenAPI
               path.startsWith("/webjars/") ||          // Swagger 静态资源
               path.startsWith("/doc.html");            // Knife4j 文档
    }
    
    /**
     * 提取 Token
     */
    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        
        // 从 Authorization 头获取
        String authHeader = headers.getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 从 token 头获取
        String tokenHeader = headers.getFirst("token");
        if (tokenHeader != null) {
            return tokenHeader;
        }
        
        // 从查询参数获取
        String tokenParam = request.getQueryParams().getFirst("token");
        if (tokenParam != null) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * 验证 Token
     */
    private boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return TokenUtil.validateToken(token);
    }
    
    /**
     * 错误处理
     */
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":" + status.value() + ",\"message\":\"" + message + "\"}";
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(bytes)));
    }
    
    @Override
    public int getOrder() {
        // 设置过滤器优先级，数字越小优先级越高
        return -100;
    }
}
