package com.bit.iot.data.model.request;

import lombok.Data;

import java.util.List;

@Data
public class RuleDataSourceQuery {

    private String deviceId;
    private String deviceName;
    private List<String> pointCodes;
    private String timeRangeStart;
    private String timeRangeEnd;
}
