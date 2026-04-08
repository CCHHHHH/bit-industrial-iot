package com.bit.iot.simulator.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("simulator_task")
/**
 * 模拟任务实体。
 */
public class SimulatorTask implements Serializable {
    /**
     * 任务主键。
     */
    @TableId("id")
    private String id;

    /**
     * 任务名称。
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 关联设备主键。
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 协议类型。
     */
    @TableField("protocol_type")
    private String protocolType;

    /**
     * 任务状态。
     */
    @TableField("task_status")
    private String taskStatus;

    /**
     * 发送频率，单位毫秒。
     */
    @TableField("frequency_ms")
    private Long frequencyMs;

    /**
     * HTTP 发送地址。
     */
    @TableField("http_url")
    private String httpUrl;

    /**
     * HTTP 请求方法。
     */
    @TableField("http_method")
    private String httpMethod;

    /**
     * MQTT Broker 地址。
     */
    @TableField("mqtt_broker_url")
    private String mqttBrokerUrl;

    /**
     * MQTT 主题。
     */
    @TableField("mqtt_topic")
    private String mqttTopic;

    /**
     * MQTT 用户名。
     */
    @TableField("mqtt_username")
    private String mqttUsername;

    /**
     * MQTT 密码。
     */
    @TableField("mqtt_password")
    private String mqttPassword;

    /**
     * Kafka 集群地址。
     */
    @TableField("kafka_bootstrap_servers")
    private String kafkaBootstrapServers;

    /**
     * Kafka 主题。
     */
    @TableField("kafka_topic")
    private String kafkaTopic;

    /**
     * Modbus 主机地址。
     */
    @TableField("modbus_host")
    private String modbusHost;

    /**
     * Modbus 端口。
     */
    @TableField("modbus_port")
    private Integer modbusPort;

    /**
     * Modbus 单元标识。
     */
    @TableField("modbus_unit_id")
    private Integer modbusUnitId;

    /**
     * 报文模板。
     */
    @TableField("payload_template")
    private String payloadTemplate;

    /**
     * 最近一次发送时间。
     */
    @TableField("last_sent_time")
    private Date lastSentTime;

    /**
     * 最近一次错误信息。
     */
    @TableField("last_error")
    private String lastError;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间。
     */
    @TableField("update_time")
    private Date updateTime;
}
