package com.bit.iot.rule.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class RuleExecutionLogVO {
    private String id;
    private String ruleId;
    private String windowKey;
    private Date startTime;
    private Date endTime;
    private Integer execStatus;
    private String resultData;
    private String errorMsg;
    private Long durationMs;
}
