package com.bit.iot.security.aspect;

import com.bit.iot.security.annotation.RequirePermission;
import com.bit.iot.security.context.UserContext;
import com.bit.iot.security.context.UserContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 权限验证切面
 * 
 * @author chenhao
 * @date 2026/3/9
 */
@Aspect
@Component
public class PermissionAspect {
    
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        // 获取当前用户上下文
        UserContext context = UserContextHolder.getContext();
        
        // 检查是否需要登录
        if (requirePermission.requireLogin() && context == null) {
            throw new RuntimeException("未登录或登录已过期");
        }
        
        // 如果没有指定权限要求，直接放行
        String[] requiredPermissions = requirePermission.value();
        if (requiredPermissions.length == 0) {
            return joinPoint.proceed();
        }
        
        // 检查用户是否有需要的权限
        if (context != null && context.getPermissions() != null) {
            List<String> userPermissions = context.getPermissions();
            boolean hasPermission = Arrays.stream(requiredPermissions)
                    .anyMatch(userPermissions::contains);
            
            if (!hasPermission) {
                throw new RuntimeException("没有访问权限");
            }
        } else {
            throw new RuntimeException("没有访问权限");
        }
        
        return joinPoint.proceed();
    }
}
