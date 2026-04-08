package com.bit.iot.integration.model.vo;

import lombok.Data;

@Data
public class IntegrationConfigParamVO {
    private String id;
    private String integrationId;
    private String paramKey;
    private String paramValue;
    private String paramDesc;
}
