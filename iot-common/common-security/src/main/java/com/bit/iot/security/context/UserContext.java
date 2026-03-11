package com.bit.iot.security.context;

import lombok.Data;

/**
 * 用户上下文信息
 * 
 * @author chenhao
 * @date 2026/3/9
 */
@Data
public class UserContext {
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 角色列表
     */
    private java.util.List<String> roles;
    
    /**
     * 权限列表
     */
    private java.util.List<String> permissions;
    
    public UserContext() {
    }
    
    public UserContext(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
