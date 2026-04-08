package com.bit.iot.device.model.vo;

import lombok.Data;

@Data
public class DeviceStationCodeVO {
    private String id;
    private String deviceId;
    private String stationCode;
    private String stationName;
    private String standardStationCode;
    private String stationDesc;
}
