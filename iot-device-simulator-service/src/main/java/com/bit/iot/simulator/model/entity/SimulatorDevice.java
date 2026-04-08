package com.bit.iot.simulator.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("simulator_device")
/**
 * 模拟设备实体。
 */
public class SimulatorDevice implements Serializable {
    /**
     * 设备主键。
     */
    @TableId("id")
    private String id;

    /**
     * 设备名称。
     */
    @TableField("device_name")
    private String deviceName;

    /**
     * 设备编码。
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 设备状态。
     */
    @TableField("device_status")
    private String deviceStatus;

    /**
     * 设备类型。
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * 设备安装位置。
     */
    @TableField("device_location")
    private String deviceLocation;

    /**
     * 设备 IP 地址。
     */
    @TableField("device_ip")
    private String deviceIp;

    /**
     * 设备 MAC 地址。
     */
    @TableField("device_mac")
    private String deviceMac;

    /**
     * 固件版本。
     */
    @TableField("firmware_version")
    private String firmwareVersion;

    /**
     * 备注信息。
     */
    @TableField("remark")
    private String remark;

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
