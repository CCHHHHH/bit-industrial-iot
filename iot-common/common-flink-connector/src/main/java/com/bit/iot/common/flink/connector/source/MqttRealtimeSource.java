package com.bit.iot.common.flink.connector.source;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.common.flink.connector.model.DeviceDataEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.source.legacy.RichParallelSourceFunction;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * MQTT 实时 Source
 * <p>
 * 订阅 MQTT 主题，实时接收设备上报数据。
 * 主题格式约定：devices/{deviceId}/telemetry
 * 消息体 JSON：{"pointCode":"P001","value":23.5,"quality":0,"ts":1711526400000}
 * </p>
 * <p>
 * 仅处理本规则关注的设备和测点。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class MqttRealtimeSource extends RichParallelSourceFunction<DeviceDataEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MqttRealtimeSource.class);

    private final RuleJobConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private transient volatile boolean isRunning = true;
    private transient MqttClient mqttClient;

    /** 缓冲队列：MQTT 回调线程写入 → Flink Source 线程读取 */
    private transient LinkedBlockingQueue<DeviceDataEvent> eventQueue;

    /** 本规则关注的设备 ID 集合（用于快速过滤） */
    private transient Set<String> relevantDeviceIds;
    private transient Map<String, Set<String>> relevantPointCodes;
    private transient long droppedCount;

    public MqttRealtimeSource(RuleJobConfig config) {
        this.config = config;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        eventQueue = new LinkedBlockingQueue<>(100_000);

        relevantDeviceIds = new HashSet<>();
        relevantPointCodes = new HashMap<>();
        if (config.getDataSources() != null) {
            config.getDataSources().forEach(ds -> {
                relevantDeviceIds.add(ds.getDeviceId());
                relevantPointCodes.put(ds.getDeviceId(),
                        ds.getPointCodes() == null ? Set.of() : new HashSet<>(ds.getPointCodes()));
            });
        }

        RuleJobConfig.MqttConfig mqtt = config.getMqttConfig();
        if (mqtt == null || mqtt.getBrokerUrl() == null) {
            LOG.warn("MQTT 配置为空，MqttRealtimeSource 不启动");
            return;
        }

        String clientId = "flink-rule-" + config.getRuleId() + "-"
                + getRuntimeContext().getTaskInfo().getIndexOfThisSubtask() + "-"
                + UUID.randomUUID().toString().substring(0, 8);

        mqttClient = new MqttClient(mqtt.getBrokerUrl(), clientId, new MemoryPersistence());

        MqttConnectionOptions options = new MqttConnectionOptions();
        if (mqtt.getUsername() != null) {
            options.setUserName(mqtt.getUsername());
        }
        if (mqtt.getPassword() != null) {
            options.setPassword(mqtt.getPassword().getBytes(StandardCharsets.UTF_8));
        }
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                LOG.warn("MQTT 连接断开: {}", disconnectResponse.getReasonString());
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                LOG.error("MQTT 错误", exception);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                try {
                    // 解析 topic: devices/{deviceId}/telemetry
                    String[] parts = topic.split("/");
                    if (parts.length < 3) return;
                    String deviceId = parts[1];

                    if (!relevantDeviceIds.contains(deviceId)) return;

                    JsonNode node = objectMapper.readTree(message.getPayload());
                    if (!node.hasNonNull("pointCode") || !node.hasNonNull("value")) {
                        LOG.warn("MQTT 消息缺少必要字段: topic={}", topic);
                        return;
                    }
                    String pointCode = node.get("pointCode").asText();
                    Set<String> allowedPoints = relevantPointCodes.getOrDefault(deviceId, Set.of());
                    if (!allowedPoints.isEmpty() && !allowedPoints.contains(pointCode)) {
                        return;
                    }
                    double value = node.get("value").asDouble();
                    int quality = node.has("quality") ? node.get("quality").asInt() : 0;
                    long ts = node.has("ts") ? node.get("ts").asLong() : System.currentTimeMillis();

                    if (!eventQueue.offer(new DeviceDataEvent(deviceId, pointCode, ts, value, quality))) {
                        droppedCount++;
                        if (droppedCount % 100 == 1) {
                            LOG.warn("MQTT 事件队列已满，累计丢弃 {} 条", droppedCount);
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("解析 MQTT 消息失败: {}", e.getMessage());
                }
            }

            @Override
            public void deliveryComplete(IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LOG.info("MQTT {}连接成功: {}", reconnect ? "重新" : "", serverURI);
                try {
                    mqttClient.subscribe(mqtt.getTopicPattern(), 1);
                } catch (MqttException e) {
                    LOG.error("MQTT 订阅失败", e);
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

        mqttClient.connect(options);
        LOG.info("MqttRealtimeSource 已启动，订阅: {}", mqtt.getTopicPattern());
    }

    @Override
    public void run(SourceContext<DeviceDataEvent> ctx) throws Exception {
        while (isRunning) {
            DeviceDataEvent event = eventQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (event != null) {
                synchronized (ctx.getCheckpointLock()) {
                    ctx.collectWithTimestamp(event, event.getTimestamp());
                }
            }
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    @Override
    public void close() throws Exception {
        isRunning = false;
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
            mqttClient.close();
        }
    }
}
