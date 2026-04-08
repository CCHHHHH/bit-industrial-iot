package com.bit.iot.simulator.service.protocol;

import com.bit.iot.simulator.model.entity.SimulatorDevice;
import com.bit.iot.simulator.model.entity.SimulatorPoint;
import com.bit.iot.simulator.model.entity.SimulatorTask;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
/**
 * 协议发送上下文。
 */
public class ProtocolPayload {
    /**
     * 当前任务。
     */
    private SimulatorTask task;
    /**
     * 当前设备。
     */
    private SimulatorDevice device;
    /**
     * 本次参与发送的测点集合。
     */
    private List<SimulatorPoint> points;
    /**
     * 实际发送的负载内容。
     */
    private Map<String, Object> payload;
    /**
     * 数据生成时间。
     */
    private Date generatedTime;
}
