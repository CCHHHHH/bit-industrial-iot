package com.bit.iot.simulator.model.request;

import lombok.Data;

@Data
/**
 * 模拟历史数据查询请求。
 */
public class SimulatorDataHistoryRequest {
    /**
     * 当前页码。
     */
    private Integer current = 1;
    /**
     * 每页条数。
     */
    private Integer size = 10;
    /**
     * 设备主键。
     */
    private String deviceId;
    /**
     * 测点编码。
     */
    private String pointCode;
    /**
     * 协议类型。
     */
    private String protocolType;
}
