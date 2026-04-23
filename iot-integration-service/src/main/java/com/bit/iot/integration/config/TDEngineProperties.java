package com.bit.iot.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tdengine")
public class TDEngineProperties {

    private String jdbcUrl;

    private String username = "root";

    private String password = "taosdata";

    private String superTable = "point_data";
}
