package com.bit.iot.simulator.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * 模拟设备新增和修改请求。
 */
public class SimulatorDeviceRequest {
    /**
     * 设备主键。
     */
    private String id;
    /**
     * 设备名称。
     */
    @NotBlank
    private String deviceName;
    /**
     * 设备编码。
     */
    @NotBlank
    private String deviceCode;
    /**
     * 设备状态。
     */
    private String deviceStatus;
    /**
     * 设备类型。
     */
    @NotBlank
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
}
