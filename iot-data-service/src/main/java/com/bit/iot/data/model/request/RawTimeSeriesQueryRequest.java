package com.bit.iot.data.model.request;

import lombok.Data;

import java.util.List;

@Data
public class RawTimeSeriesQueryRequest {

    private List<String> deviceIds;
    private List<String> pointCodes;
    private Long startTime;
    private Long endTime;
    private Integer limit;
}
