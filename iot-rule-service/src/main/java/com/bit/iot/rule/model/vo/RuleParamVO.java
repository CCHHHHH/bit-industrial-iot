package com.bit.iot.rule.model.vo;

import lombok.Data;

@Data
public class RuleParamVO {
    private String id;
    private String ruleId;
    private String paramKey;
    private String paramValue;
    private String paramDesc;
}
