package com.bit.iot.data.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("IoT Data Service API")
                .description("工业物联网时序数据查询服务接口文档")
                .version("1.0")
                .contact(new Contact().name("chenhao")));
    }
}
