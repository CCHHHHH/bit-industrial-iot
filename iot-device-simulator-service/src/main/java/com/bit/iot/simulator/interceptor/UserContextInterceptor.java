package com.bit.iot.simulator.interceptor;

import com.bit.iot.security.context.UserContext;
import com.bit.iot.security.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
/**
 * 用户上下文拦截器。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    /**
     * 解析请求头中的用户上下文并写入线程上下文。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 处理器
     * @return 始终返回 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
            // 仅在核心身份字段完整时写入上下文，避免半残缺身份污染线程变量。
            UserContext context = new UserContext(userId, username);
            context.setRoles(parseListHeader(request.getHeader("X-User-Roles")));
            context.setPermissions(parseListHeader(request.getHeader("X-User-Permissions")));
            UserContextHolder.setContext(context);
        }
        return true;
    }

    /**
     * 请求处理完成后清理线程上下文。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 处理器
     * @param modelAndView 视图模型
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        UserContextHolder.clearContext();
    }

    /**
     * 请求最终结束后再次清理线程上下文。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 处理器
     * @param ex 异常信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clearContext();
    }

    /**
     * 解析逗号分隔的请求头列表。
     *
     * @param header 请求头值
     * @return 解析后的字符串列表
     */
    private List<String> parseListHeader(String header) {
        if (!StringUtils.hasText(header)) {
            return Collections.emptyList();
        }
        String[] items = header.split(",");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String trimmed = item.trim();
            if (StringUtils.hasText(trimmed)) {
                // 过滤空白片段，避免请求头中的多余分隔符污染角色与权限集合。
                result.add(trimmed);
            }
        }
        return result;
    }
}
