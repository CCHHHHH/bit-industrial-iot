package com.bit.iot.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tdengine")
public class TDEngineProperties {

    private String jdbcUrl;
    private String username = "root";
    private String password = "taosdata";
    private String superTable = "device_data";
    private Integer connectTimeoutSeconds = 5;
    private Integer queryTimeoutSeconds = 30;
    private Integer maxLimitPerQuery = 5000;
}
