package com.bit.iot.security.context;

/**
 * 用户上下文持有者
 * 使用 ThreadLocal 存储当前请求的用户信息
 * 
 * @author chenhao
 * @date 2026/3/9
 */
public class UserContextHolder {
    
    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置用户上下文
     */
    public static void setContext(UserContext context) {
        CONTEXT_HOLDER.set(context);
    }
    
    /**
     * 获取用户上下文
     */
    public static UserContext getContext() {
        return CONTEXT_HOLDER.get();
    }
    
    /**
     * 清除用户上下文
     * 必须在请求结束后调用，防止内存泄漏
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
