package com.bit.iot.simulator.service.protocol.impl;

import com.bit.iot.simulator.model.entity.SimulatorTask;
import com.bit.iot.simulator.model.enums.ProtocolTypeEnum;
import com.bit.iot.simulator.service.protocol.ProtocolPayload;
import com.bit.iot.simulator.service.protocol.ProtocolSendResult;
import com.bit.iot.simulator.service.protocol.ProtocolSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
/**
 * Kafka 协议发送器。
 */
public class KafkaProtocolSender implements ProtocolSender {

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 返回支持的协议类型。
     *
     * @return Kafka 协议
     */
    @Override
    public ProtocolTypeEnum protocolType() {
        return ProtocolTypeEnum.KAFKA;
    }

    /**
     * 通过 Kafka 发送模拟数据。
     *
     * @param payload 协议发送上下文
     * @return 发送结果
     * @throws Exception 发送异常
     */
    @Override
    public ProtocolSendResult send(ProtocolPayload payload) throws Exception {
        SimulatorTask task = payload.getTask();
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, task.getKafkaBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        String body = objectMapper.writeValueAsString(payload.getPayload());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            // 同步等待发送完成，确保调用方能拿到明确的成功或失败结果。
            producer.send(new ProducerRecord<>(task.getKafkaTopic(), payload.getDevice().getDeviceCode(), body)).get();
        }
        return ProtocolSendResult.success("Kafka send success");
    }
}
