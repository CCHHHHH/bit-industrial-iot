package com.bit.iot.integration.model.request;

import lombok.Data;

@Data
public class IntegrationPluginRequest {
    private String id;
    private String pluginName;
    private String pluginDescription;
    private String pluginPath;
    private String pluginType;
    private Integer pluginStatus;
    private String pluginVersion;
    private Integer pluginSize;
}
