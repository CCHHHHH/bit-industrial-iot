package com.bit.iot.integration.model.request;

import lombok.Data;

@Data
public class IntegrationConfigParamRequest {
    private String id;
    private String integrationId;
    private String paramKey;
    private String paramValue;
    private String paramDesc;
}
