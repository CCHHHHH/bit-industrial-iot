package com.bit.iot.simulator.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("simulator_point")
/**
 * 模拟测点实体。
 */
public class SimulatorPoint implements Serializable {
    /**
     * 测点主键。
     */
    @TableId("id")
    private String id;

    /**
     * 关联设备主键。
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
     * 计量单位。
     */
    @TableField("unit")
    private String unit;

    /**
     * 最小值。
     */
    @TableField("min_value")
    private Double minValue;

    /**
     * 最大值。
     */
    @TableField("max_value")
    private Double maxValue;

    /**
     * 保留小数位数。
     */
    @TableField("precision_scale")
    private Integer precisionScale;

    /**
     * 数据质量码。
     */
    @TableField("quality")
    private Integer quality;

    /**
     * Modbus 寄存器地址。
     */
    @TableField("register_address")
    private Integer registerAddress;

    /**
     * Modbus 寄存器类型。
     */
    @TableField("register_type")
    private String registerType;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间。
     */
    @TableField("update_time")
    private Date updateTime;
}
