package com.bit.iot.rule.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class RuleDataSourceVO {
    private String id;
    private String ruleId;
    private String deviceId;
    private String deviceName;
    private String pointCodes;
    private String timeRangeStart;
    private String timeRangeEnd;
    private Date createTime;
}
