package com.bit.iot.rule.model.request;

import lombok.Data;

@Data
public class RuleAlgorithmRequest {
    private String id;
    private String algorithmName;
    private String algorithmDesc;
    private String algorithmType;
    private String algorithmPath;
    private String algorithmClass;
    private String algorithmVersion;
    private Integer algorithmStatus;
    private Long fileSize;
}
