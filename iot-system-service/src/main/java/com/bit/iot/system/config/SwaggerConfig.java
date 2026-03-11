package com.bit.iot.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 (Swagger 3.0) 配置类
 * 适配 Spring Boot 3.x + Jakarta EE 9+
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IoT System Service API")
                        .description("工业物联网系统服务接口文档 - OpenAPI 3.0")
                        .version("1.0")
                        .contact(new Contact()
                                .name("chenhao")
                                .email("")
                                .url("")));
    }

    @Bean
    public GlobalOpenApiCustomizer globalCustomizer() {
        return openApi -> {
            // 可以在这里添加全局的自定义配置
            // 例如：添加统一的请求头参数、响应码说明等
        };
    }
}
