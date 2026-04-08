package com.bit.iot.simulator.model.vo;

import lombok.Data;

import java.util.Date;

@Data
/**
 * 模拟任务视图对象。
 */
public class SimulatorTaskVO {
    /**
     * 任务主键。
     */
    private String id;
    /**
     * 任务名称。
     */
    private String taskName;
    /**
     * 关联设备主键。
     */
    private String deviceId;
    /**
     * 协议类型。
     */
    private String protocolType;
    /**
     * 任务状态。
     */
    private String taskStatus;
    /**
     * 发送频率，单位毫秒。
     */
    private Long frequencyMs;
    /**
     * HTTP 发送地址。
     */
    private String httpUrl;
    /**
     * HTTP 请求方法。
     */
    private String httpMethod;
    /**
     * MQTT Broker 地址。
     */
    private String mqttBrokerUrl;
    /**
     * MQTT 主题。
     */
    private String mqttTopic;
    /**
     * Kafka 集群地址。
     */
    private String kafkaBootstrapServers;
    /**
     * Kafka 主题。
     */
    private String kafkaTopic;
    /**
     * Modbus 主机地址。
     */
    private String modbusHost;
    /**
     * Modbus 端口。
     */
    private Integer modbusPort;
    /**
     * Modbus 单元标识。
     */
    private Integer modbusUnitId;
    /**
     * 报文模板。
     */
    private String payloadTemplate;
    /**
     * 最近一次发送时间。
     */
    private Date lastSentTime;
    /**
     * 最近一次错误信息。
     */
    private String lastError;
    /**
     * 创建时间。
     */
    private Date createTime;
    /**
     * 更新时间。
     */
    private Date updateTime;
}
