package com.bit.iot.device.model.request;

import lombok.Data;

@Data
public class DeviceStationCodeRequest {
    private String id;
    private String deviceId;
    private String stationCode;
    private String stationName;
    private String standardStationCode;
    private String stationDesc;
}
