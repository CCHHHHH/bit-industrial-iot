package com.bit.iot.simulator.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * 模拟任务新增和修改请求。
 */
public class SimulatorTaskRequest {
    /**
     * 任务主键。
     */
    private String id;
    /**
     * 任务名称。
     */
    @NotBlank
    private String taskName;
    /**
     * 关联设备主键。
     */
    @NotBlank
    private String deviceId;
    /**
     * 协议类型。
     */
    @NotBlank
    private String protocolType;
    /**
     * 发送频率，单位毫秒。
     */
    @Min(100)
    private Long frequencyMs = 1000L;
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
     * MQTT 用户名。
     */
    private String mqttUsername;
    /**
     * MQTT 密码。
     */
    private String mqttPassword;
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
}
