package com.bit.iot.simulator.model.vo;

import lombok.Data;

import java.util.Date;

@Data
/**
 * 模拟测点视图对象。
 */
public class SimulatorPointVO {
    /**
     * 测点主键。
     */
    private String id;
    /**
     * 关联设备主键。
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
     * 计量单位。
     */
    private String unit;
    /**
     * 最小值。
     */
    private Double minValue;
    /**
     * 最大值。
     */
    private Double maxValue;
    /**
     * 保留小数位数。
     */
    private Integer precisionScale;
    /**
     * 数据质量码。
     */
    private Integer quality;
    /**
     * Modbus 寄存器地址。
     */
    private Integer registerAddress;
    /**
     * Modbus 寄存器类型。
     */
    private String registerType;
    /**
     * 创建时间。
     */
    private Date createTime;
    /**
     * 更新时间。
     */
    private Date updateTime;
}
