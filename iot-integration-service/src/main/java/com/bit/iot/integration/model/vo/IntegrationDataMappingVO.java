package com.bit.iot.integration.model.vo;

import lombok.Data;

@Data
public class IntegrationDataMappingVO {
    private String id;
    private String integrationId;
    private String sourceData;
    private String mappingType;
    private Long schedulerTime;
    private String schedulerUnit;
}
