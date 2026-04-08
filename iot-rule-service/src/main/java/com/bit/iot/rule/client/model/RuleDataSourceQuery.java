package com.bit.iot.rule.client.model;

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
