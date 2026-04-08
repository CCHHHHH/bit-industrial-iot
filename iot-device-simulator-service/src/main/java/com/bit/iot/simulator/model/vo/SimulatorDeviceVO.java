package com.bit.iot.simulator.model.vo;

import lombok.Data;

import java.util.Date;

@Data
/**
 * 模拟设备视图对象。
 */
public class SimulatorDeviceVO {
    /**
     * 设备主键。
     */
    private String id;
    /**
     * 设备名称。
     */
    private String deviceName;
    /**
     * 设备编码。
     */
    private String deviceCode;
    /**
     * 设备状态。
     */
    private String deviceStatus;
    /**
     * 设备类型。
     */
    private String deviceType;
    /**
     * 设备安装位置。
     */
    private String deviceLocation;
    /**
     * 设备 IP 地址。
     */
    private String deviceIp;
    /**
     * 设备 MAC 地址。
     */
    private String deviceMac;
    /**
     * 固件版本。
     */
    private String firmwareVersion;
    /**
     * 备注信息。
     */
    private String remark;
    /**
     * 创建时间。
     */
    private Date createTime;
    /**
     * 更新时间。
     */
    private Date updateTime;
}
