package com.bit.iot.rule.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警记录表。
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("alarm_record")
@Schema(description = "告警记录")
public class AlarmRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("rule_id")
    private String ruleId;

    @TableField("rule_name")
    private String ruleName;

    @TableField("device_id")
    private String deviceId;

    @TableField("device_name")
    private String deviceName;

    @TableField("point_code")
    private String pointCode;

    @TableField("dedup_key")
    private String dedupKey;

    @TableField("alarm_title")
    private String alarmTitle;

    @TableField("alarm_message")
    private String alarmMessage;

    @TableField("alarm_level")
    private String alarmLevel;

    @TableField("alarm_status")
    private String alarmStatus;

    @TableField("trigger_count")
    private Integer triggerCount;

    @TableField("first_trigger_time")
    private Date firstTriggerTime;

    @TableField("last_trigger_time")
    private Date lastTriggerTime;

    @TableField("resolved_time")
    private Date resolvedTime;

    @TableField("metric_name")
    private String metricName;

    @TableField("metric_value")
    private String metricValue;

    @TableField("result_data")
    private String resultData;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
