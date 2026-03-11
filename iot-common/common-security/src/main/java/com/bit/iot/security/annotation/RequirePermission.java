package com.bit.iot.security.annotation;

import java.lang.annotation.*;

/**
 * 需要权限的注解
 * 
 * @author chenhao
 * @date 2026/3/9
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * 需要的权限代码
     */
    String[] value() default {};
    
    /**
     * 是否需要登录（默认需要）
     */
    boolean requireLogin() default true;
}
