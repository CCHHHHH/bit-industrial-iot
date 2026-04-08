package com.bit.iot.rule.client.model;

import com.bit.iot.common.flink.DataPoint;
import lombok.Data;

import java.util.Date;

@Data
public class TimeSeriesPointResponse {

    private String deviceId;
    private String pointCode;
    private Long timestamp;
    private Double value;
    private Integer quality;

    public DataPoint toDataPoint() {
        return new DataPoint(deviceId, pointCode, new Date(timestamp), value, quality);
    }
}
