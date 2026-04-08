package com.bit.iot.flink.job;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.common.flink.connector.model.AlgorithmOutputEvent;
import com.bit.iot.common.flink.connector.model.DeviceDataEvent;
import com.bit.iot.common.flink.connector.sink.MySQLAlarmSink;
import com.bit.iot.common.flink.connector.sink.MySQLExecutionLogSink;
import com.bit.iot.common.flink.connector.sink.TDEngineResultSink;
import com.bit.iot.common.flink.connector.source.MqttRealtimeSource;
import com.bit.iot.common.flink.connector.source.TDEngineJdbcSource;
import com.bit.iot.common.flink.connector.window.DynamicWindowAssigner;
import com.bit.iot.flink.job.process.AlgorithmWindowFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Flink Job 唯一入口
 * <p>
 * 通过 {@code --ruleConfig <base64_json>} 接收完整规则配置。
 * 构建数据管道：Source → KeyBy → Window → AlgorithmProcess → Sink
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class RuleJobEntrypoint {

    private static final Logger LOG = LoggerFactory.getLogger(RuleJobEntrypoint.class);

    public static void main(String[] args) throws Exception {
        // =============================================================
        // 1. 解析参数
        // =============================================================
        String configBase64 = getRequiredArg(args, "ruleConfig");
        String configJson = new String(
                Base64.getDecoder().decode(configBase64), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        RuleJobConfig config = mapper.readValue(configJson, RuleJobConfig.class);

        LOG.info("启动 Flink Job: ruleId={}, ruleName={}, triggerType={}, windowType={}",
                config.getRuleId(), config.getRuleName(),
                config.getTriggerType(), config.getWindowType());

        // =============================================================
        // 2. 构建执行环境
        // =============================================================
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(config.getParallelism());

        env.getConfig().setAutoWatermarkInterval(200L);

        // Checkpoint 配置：每 60s，Exactly-Once
        env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);
        CheckpointConfig cpConfig = env.getCheckpointConfig();
        cpConfig.setMinPauseBetweenCheckpoints(30_000L);
        cpConfig.setCheckpointTimeout(120_000L);
        cpConfig.setMaxConcurrentCheckpoints(1);
        cpConfig.setExternalizedCheckpointRetention(ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);

        // =============================================================
        // 3. Source：根据触发类型选择 TDEngine JDBC 轮询 或 MQTT 实时
        // =============================================================
        WatermarkStrategy<DeviceDataEvent> watermarkStrategy =
                WatermarkStrategy.<DeviceDataEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, recordTimestamp) -> event.getTimestamp());

        DataStream<DeviceDataEvent> source;

        if (config.getMockSourceConfig() != null
                && config.getMockSourceConfig().isEnabled()
                && config.getMockSourceConfig().getEvents() != null
                && !config.getMockSourceConfig().getEvents().isEmpty()) {
            List<DeviceDataEvent> mockEvents = config.getMockSourceConfig().getEvents().stream()
                    .map(event -> new DeviceDataEvent(
                            event.getDeviceId(),
                            event.getPointCode(),
                            event.getTimestamp(),
                            event.getValue(),
                            event.getQuality()))
                    .toList();
            source = env.fromCollection(mockEvents)
                    .name("Mock-Source")
                    .assignTimestampsAndWatermarks(watermarkStrategy);
        } else if ("realtime".equalsIgnoreCase(config.getTriggerType())
                && config.getMqttConfig() != null
                && config.getMqttConfig().getBrokerUrl() != null) {
            source = env.addSource(new MqttRealtimeSource(config))
                    .name("MQTT-Source")
                    .assignTimestampsAndWatermarks(watermarkStrategy);
        } else {
            long pollInterval = config.getWindowSizeMs() > 0
                    ? Math.max(config.getWindowSizeMs() / 4, 5000L)
                    : 15_000L;
            source = env.addSource(new TDEngineJdbcSource(config, pollInterval))
                    .name("TDEngine-JDBC-Source")
                    .assignTimestampsAndWatermarks(watermarkStrategy);
        }

        // =============================================================
        // 4. KeyBy：根据分组策略选择
        // =============================================================
        KeyedStream<DeviceDataEvent, String> keyed;

        if ("device".equalsIgnoreCase(config.getKeyStrategy())) {
            keyed = source.keyBy(DeviceDataEvent::getDeviceId);
        } else {
            keyed = source.keyBy(e -> e.getDeviceId() + "#" + e.getPointCode());
        }

        // =============================================================
        // 5. Window + ProcessFunction
        // =============================================================
        DataStream<AlgorithmOutputEvent> result = DynamicWindowAssigner
                .applyWindow(keyed, config)
                .process(new AlgorithmWindowFunction(
                        config.getRuleId(),
                        config.getAlgorithmType(),
                        config.getAlgorithmPath(),
                        config.getAlgorithmClass(),
                        config.getRuleParams()
                ))
                .name("Algorithm-Process");

        // =============================================================
        // 6. Sink：结果多路输出
        // =============================================================
        if (config.getTdengineConfig() != null
                && config.getTdengineConfig().isEnabled()
                && config.getTdengineConfig().getJdbcUrl() != null
                && !config.getTdengineConfig().getJdbcUrl().isBlank()) {
            result.addSink(new TDEngineResultSink(config.getTdengineConfig()))
                    .name("TDEngine-Result-Sink");
        }

        if (config.getMysqlConfig() != null && config.getMysqlConfig().getJdbcUrl() != null) {
            result.addSink(new MySQLExecutionLogSink(config.getMysqlConfig()))
                    .name("MySQL-Log-Sink");
            result.addSink(new MySQLAlarmSink(config))
                    .name("MySQL-Alarm-Sink");
        }

        result.print().name("Debug-Print-Sink");

        // =============================================================
        // 7. 启动 Job
        // =============================================================
        String jobName = "RuleJob-" + config.getRuleId() + "-" + config.getRuleName();
        env.execute(jobName);
    }

    private static String getRequiredArg(String[] args, String key) {
        String option = "--" + key;
        for (int i = 0; i < args.length - 1; i++) {
            if (option.equals(args[i])) {
                return args[i + 1];
            }
        }
        throw new IllegalArgumentException("缺少必填参数 " + option + "，当前参数: " + Arrays.toString(args));
    }
}
