package com.bit.iot.simulator.config;

import com.bit.iot.simulator.interceptor.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/**
 * Web MVC 配置类。
 */
public class WebConfig implements WebMvcConfigurer {

    /**
     * 用户上下文拦截器。
     */
    @Autowired
    private UserContextInterceptor userContextInterceptor;

    /**
     * 注册全局拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/favicon.ico",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/**",
                        "/v3/**",
                        "/doc.html"
                );
    }
}
