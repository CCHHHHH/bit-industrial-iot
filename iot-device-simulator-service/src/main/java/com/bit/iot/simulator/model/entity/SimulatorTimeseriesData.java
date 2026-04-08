package com.bit.iot.simulator.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("simulator_timeseries_data")
/**
 * 模拟时序数据实体。
 */
public class SimulatorTimeseriesData implements Serializable {
    /**
     * 数据主键。
     */
    @TableId("id")
    private String id;

    /**
     * 任务主键。
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 设备主键。
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 测点编码。
     */
    @TableField("point_code")
    private String pointCode;

    /**
     * 测点名称。
     */
    @TableField("point_name")
    private String pointName;

    /**
     * 测点值。
     */
    @TableField("point_value")
    private Double pointValue;

    /**
     * 单位。
     */
    @TableField("unit")
    private String unit;

    /**
     * 质量码。
     */
    @TableField("quality")
    private Integer quality;

    /**
     * 协议类型。
     */
    @TableField("protocol_type")
    private String protocolType;

    /**
     * 数据生成时间。
     */
    @TableField("generated_time")
    private Date generatedTime;
}
