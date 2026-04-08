package com.bit.iot.device.model.request;

import lombok.Data;

@Data
public class DeviceRequest {
    private String id;
    private String deviceName;
    private String deviceCode;
    private String status;
    private String deviceType;
    private String catalogueId;
}
