package com.bit.iot.rule.service.support;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 告警写入命令。
 */
@Data
public class AlarmUpsertCommand {

    private String ruleId;
    private String ruleName;
    private String deviceId;
    private String deviceName;
    private String pointCode;
    private String sourceType;
    private String sourceId;
    private String dedupKey;
    private String alarmTitle;
    private String alarmMessage;
    private String alarmLevel;
    private String metricName;
    private String metricValue;
    private Map<String, Object> resultData;
    private Date triggerTime;
}
