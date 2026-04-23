package com.bit.iot.integration.model.dto;

import lombok.Data;

@Data
public class TimeSeriesCollectResultDTO {

    private String integrationId;

    private Integer mappingCount = 0;

    private Integer pointCount = 0;

    private Integer writtenCount = 0;

    private String message;

    public void add(TimeSeriesCollectResultDTO item) {
        if (item == null) {
            return;
        }
        this.mappingCount += item.getMappingCount() == null ? 0 : item.getMappingCount();
        this.pointCount += item.getPointCount() == null ? 0 : item.getPointCount();
        this.writtenCount += item.getWrittenCount() == null ? 0 : item.getWrittenCount();
    }
}
