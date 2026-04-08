package com.bit.iot.rule.model.request;

import lombok.Data;

@Data
public class RuleParamRequest {
    private String id;
    private String ruleId;
    private String paramKey;
    private String paramValue;
    private String paramDesc;
}
