package com.bit.iot.simulator.model.vo;

import lombok.Data;

import java.util.Date;

@Data
/**
 * 模拟时序数据视图对象。
 */
public class SimulatorTimeseriesDataVO {
    /**
     * 数据主键。
     */
    private String id;
    /**
     * 任务主键。
     */
    private String taskId;
    /**
     * 设备主键。
     */
    private String deviceId;
    /**
     * 测点编码。
     */
    private String pointCode;
    /**
     * 测点名称。
     */
    private String pointName;
    /**
     * 测点值。
     */
    private Double pointValue;
    /**
     * 计量单位。
     */
    private String unit;
    /**
     * 数据质量码。
     */
    private Integer quality;
    /**
     * 协议类型。
     */
    private String protocolType;
    /**
     * 数据生成时间。
     */
    private Date generatedTime;
}
