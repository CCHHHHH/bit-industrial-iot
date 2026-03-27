package com.bit.iot.rule.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 从网关转发的请求头中提取用户信息
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 由网关在 JWT 验证后注入以下请求头
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        // 此处可存入 ThreadLocal，供 Service 层使用
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理 ThreadLocal
    }
}
