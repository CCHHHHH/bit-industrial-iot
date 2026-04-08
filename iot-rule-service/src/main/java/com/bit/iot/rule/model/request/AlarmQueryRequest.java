package com.bit.iot.rule.model.request;

import lombok.Data;

/**
 * 告警分页查询请求。
 */
@Data
public class AlarmQueryRequest {

    private Integer current = 1;
    private Integer size = 10;
    private String keyword;
    private String alarmStatus;
    private String alarmLevel;
}
