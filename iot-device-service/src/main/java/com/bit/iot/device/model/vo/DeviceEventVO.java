package com.bit.iot.device.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class DeviceEventVO {
    private String id;
    private String deviceId;
    private Date eventTime;
    private String eventMessage;
}
