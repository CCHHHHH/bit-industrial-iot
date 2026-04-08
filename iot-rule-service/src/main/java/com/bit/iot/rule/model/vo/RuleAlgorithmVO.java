package com.bit.iot.rule.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class RuleAlgorithmVO {
    private String id;
    private String algorithmName;
    private String algorithmDesc;
    private String algorithmType;
    private String algorithmPath;
    private String algorithmClass;
    private String algorithmVersion;
    private Integer algorithmStatus;
    private Long fileSize;
    private Date createTime;
    private Date updateTime;
}
