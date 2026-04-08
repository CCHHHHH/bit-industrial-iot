package com.bit.iot.rule.model.request;

import lombok.Data;

@Data
public class RuleConfigRequest {
    private String id;
    private String ruleName;
    private String ruleDesc;
    private String algorithmId;
    private String triggerType;
    private String triggerCron;
    private String windowType;
    private Long windowSize;
    private Long windowSlide;
    private String windowUnit;
    private String keyStrategy;
    private Integer parallelism;
    private Integer ruleStatus;
    private String flinkJobId;
}
