package com.bit.iot.system.interceptor;

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

/**
 * 用户上下文拦截器
 * 从网关传递的 Header 中获取用户信息并放入 ThreadLocal
 * 
 * @author chenhao
 * @date 2026/3/9
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从网关传递的 Header 中获取用户信息
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        
        if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
            // 构建用户上下文
            UserContext context = new UserContext(userId, username);
            
            // 尝试获取角色和权限（从 Header 中）
            String rolesHeader = request.getHeader("X-User-Roles");
            String permissionsHeader = request.getHeader("X-User-Permissions");
            
            if (StringUtils.hasText(rolesHeader)) {
                List<String> roles = parseListHeader(rolesHeader);
                context.setRoles(roles);
            }
            
            if (StringUtils.hasText(permissionsHeader)) {
                List<String> permissions = parseListHeader(permissionsHeader);
                context.setPermissions(permissions);
            }
            
            // 存入 ThreadLocal
            UserContextHolder.setContext(context);
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 请求处理完成后清除上下文
        UserContextHolder.clearContext();
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 确保清除，防止内存泄漏
        UserContextHolder.clearContext();
    }
    
    /**
     * 解析列表类型的 Header
     */
    private List<String> parseListHeader(String header) {
        if (!StringUtils.hasText(header)) {
            return Collections.emptyList();
        }
        
        // 假设格式为：role1,role2,role3
        String[] items = header.split(",");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String trimmed = item.trim();
            if (StringUtils.hasText(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
