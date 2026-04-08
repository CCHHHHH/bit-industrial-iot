package com.bit.iot.simulator.service.protocol.impl;

import com.bit.iot.simulator.model.entity.SimulatorTask;
import com.bit.iot.simulator.model.enums.ProtocolTypeEnum;
import com.bit.iot.simulator.service.protocol.ProtocolPayload;
import com.bit.iot.simulator.service.protocol.ProtocolSendResult;
import com.bit.iot.simulator.service.protocol.ProtocolSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
/**
 * MQTT 协议发送器。
 */
public class MqttProtocolSender implements ProtocolSender {

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 返回支持的协议类型。
     *
     * @return MQTT 协议
     */
    @Override
    public ProtocolTypeEnum protocolType() {
        return ProtocolTypeEnum.MQTT;
    }

    /**
     * 通过 MQTT 发布模拟数据。
     *
     * @param payload 协议发送上下文
     * @return 发送结果
     * @throws Exception 发送异常
     */
    @Override
    public ProtocolSendResult send(ProtocolPayload payload) throws Exception {
        SimulatorTask task = payload.getTask();
        String body = objectMapper.writeValueAsString(payload.getPayload());
        // 为每次发送生成独立 clientId，避免并发发送时出现客户端标识冲突。
        String clientId = "simulator-" + task.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        MqttClient client = new MqttClient(task.getMqttBrokerUrl(), clientId, new MemoryPersistence());
        try {
            MqttConnectionOptions options = new MqttConnectionOptions();
            if (task.getMqttUsername() != null && !task.getMqttUsername().isBlank()) {
                options.setUserName(task.getMqttUsername());
            }
            if (task.getMqttPassword() != null && !task.getMqttPassword().isBlank()) {
                options.setPassword(task.getMqttPassword().getBytes(StandardCharsets.UTF_8));
            }
            client.connect(options);
            MqttMessage message = new MqttMessage(body.getBytes(StandardCharsets.UTF_8));
            // 使用 QoS 1 平衡可靠性与发送开销。
            message.setQos(1);
            client.publish(task.getMqttTopic(), message);
        } finally {
            // 显式断开并关闭客户端，避免连接资源泄漏。
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        }
        return ProtocolSendResult.success("MQTT publish success");
    }
}
