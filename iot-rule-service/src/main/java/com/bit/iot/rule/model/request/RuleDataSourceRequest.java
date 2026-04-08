package com.bit.iot.rule.model.request;

import lombok.Data;

@Data
public class RuleDataSourceRequest {
    private String id;
    private String ruleId;
    private String deviceId;
    private String deviceName;
    private String pointCodes;
    private String timeRangeStart;
    private String timeRangeEnd;
}
