package com.bit.iot.rule.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / Knife4j 配置
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("规则引擎服务 API")
                        .description("IoT 规则引擎：算法管理、规则配置、执行日志")
                        .version("1.0.0"));
    }
}
