package com.bit.iot.simulator.model.vo;

import lombok.Data;

import java.util.Date;

@Data
/**
 * 模拟发送日志视图对象。
 */
public class SimulatorSendLogVO {
    /**
     * 日志主键。
     */
    private String id;
    /**
     * 任务主键。
     */
    private String taskId;
    /**
     * 设备主键。
     */
    private String deviceId;
    /**
     * 协议类型。
     */
    private String protocolType;
    /**
     * 发送报文 JSON。
     */
    private String payloadJson;
    /**
     * 发送状态。
     */
    private String sendStatus;
    /**
     * 错误信息。
     */
    private String errorMessage;
    /**
     * 发送时间。
     */
    private Date sentTime;
}
