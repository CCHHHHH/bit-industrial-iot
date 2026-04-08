package com.bit.iot.rule.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class RuleConfigVO {
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
    private Date createTime;
    private Date updateTime;
}
