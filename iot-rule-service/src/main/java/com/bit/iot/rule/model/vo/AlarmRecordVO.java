package com.bit.iot.rule.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 告警返回对象。
 */
@Data
public class AlarmRecordVO {

    private String id;
    private String sourceType;
    private String sourceId;
    private String ruleId;
    private String ruleName;
    private String deviceId;
    private String deviceName;
    private String pointCode;
    private String alarmTitle;
    private String alarmMessage;
    private String alarmLevel;
    private String alarmStatus;
    private Integer triggerCount;
    private Date firstTriggerTime;
    private Date lastTriggerTime;
    private Date resolvedTime;
    private String metricName;
    private String metricValue;
    private String resultData;
    private Date createTime;
    private Date updateTime;
}
