package com.bit.iot.device.model.request;

import lombok.Data;

import java.util.Date;

@Data
public class DeviceEventRequest {
    private String id;
    private String deviceId;
    private Date eventTime;
    private String eventMessage;
}
