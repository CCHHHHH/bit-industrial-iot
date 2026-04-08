package com.bit.iot.integration.model.request;

import lombok.Data;

@Data
public class IntegrationConfigRequest {
    private String id;
    private String integrationName;
    private String pluginId;
    private Integer integrationStatus;
    private String integrationDesc;
}
