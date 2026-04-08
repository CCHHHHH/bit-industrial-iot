package com.bit.iot.simulator.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("simulator_send_log")
/**
 * 模拟发送日志实体。
 */
public class SimulatorSendLog implements Serializable {
    /**
     * 日志主键。
     */
    @TableId("id")
    private String id;

    /**
     * 任务主键。
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 设备主键。
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 协议类型。
     */
    @TableField("protocol_type")
    private String protocolType;

    /**
     * 发送报文 JSON。
     */
    @TableField("payload_json")
    private String payloadJson;

    /**
     * 发送状态。
     */
    @TableField("send_status")
    private String sendStatus;

    /**
     * 错误信息。
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 发送时间。
     */
    @TableField("sent_time")
    private Date sentTime;
}
