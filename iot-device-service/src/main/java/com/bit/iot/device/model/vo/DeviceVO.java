package com.bit.iot.device.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class DeviceVO {
    private String id;
    private String deviceName;
    private String deviceCode;
    private String status;
    private String deviceType;
    private String catalogueId;
    private Date createTime;
    private Date updateTime;
}
