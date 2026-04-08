package com.bit.iot.data.model.vo;

import com.bit.iot.common.flink.DataPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPointVO {

    private String deviceId;
    private String pointCode;
    private Long timestamp;
    private Double value;
    private Integer quality;

    public DataPoint toDataPoint() {
        return new DataPoint(deviceId, pointCode, new Date(timestamp), value, quality);
    }
}
