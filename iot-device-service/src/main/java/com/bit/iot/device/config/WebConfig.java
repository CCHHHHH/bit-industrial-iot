package com.bit.iot.device.config;

import com.bit.iot.device.interceptor.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 
 * @author chenhao
 * @date 2026/3/9
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserContextInterceptor userContextInterceptor;

    // 注意：跨域配置已移至 Gateway 统一处理，避免重复配置

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/user/login",  // 排除登录接口（网关已鉴权）
                        "/error",       // 排除错误页面
                        "/favicon.ico", // 排除图标
                        "/swagger-resources/**",  // Swagger 相关
                        "/webjars/**",           // Swagger 静态资源
                        "/v2/**",                // Swagger API
                        "/v3/**",                // OpenAPI
                        "/doc.html"              // Knife4j 文档
                );
    }
}
