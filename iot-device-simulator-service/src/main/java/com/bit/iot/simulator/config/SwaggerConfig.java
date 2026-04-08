package com.bit.iot.simulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * OpenAPI 文档配置类。
 */
public class SwaggerConfig {

    /**
     * 构建基础 OpenAPI 元数据。
     *
     * @return OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IoT Device Simulator Service API")
                        .description("设备模拟服务接口文档")
                        .version("1.0")
                        .contact(new Contact().name("chenhao")));
    }

    /**
     * 注册全局 OpenAPI 自定义器。
     *
     * @return 全局自定义器
     */
    @Bean
    public GlobalOpenApiCustomizer globalCustomizer() {
        return openApi -> {
        };
    }
}
