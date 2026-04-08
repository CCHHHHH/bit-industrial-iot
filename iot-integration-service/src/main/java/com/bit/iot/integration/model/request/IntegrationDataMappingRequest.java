package com.bit.iot.integration.model.request;

import lombok.Data;

@Data
public class IntegrationDataMappingRequest {
    private String id;
    private String integrationId;
    private String sourceData;
    private String mappingType;
    private Long schedulerTime;
    private String schedulerUnit;
}
