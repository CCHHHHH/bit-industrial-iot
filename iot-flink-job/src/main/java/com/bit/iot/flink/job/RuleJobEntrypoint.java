package com.bit.iot.flink.job;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.flink.job.model.AlgorithmOutputEvent;
import com.bit.iot.flink.job.model.DeviceDataEvent;
import com.bit.iot.flink.job.process.AlgorithmWindowFunction;
import com.bit.iot.flink.job.sink.MySQLExecutionLogSink;
import com.bit.iot.flink.job.sink.TDEngineResultSink;
import com.bit.iot.flink.job.source.MqttRealtimeSource;
import com.bit.iot.flink.job.source.TDEngineJdbcSource;
import com.bit.iot.flink.job.window.DynamicWindowAssigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

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
        ParameterTool params = ParameterTool.fromArgs(args);
        String configBase64 = params.getRequired("ruleConfig");
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

        // 自动水位线间隔
        env.getConfig().setAutoWatermarkInterval(200L);

        // Checkpoint 配置：每 60s，Exactly-Once
        env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);
        CheckpointConfig cpConfig = env.getCheckpointConfig();
        cpConfig.setMinPauseBetweenCheckpoints(30_000L);
        cpConfig.setCheckpointTimeout(120_000L);
        cpConfig.setMaxConcurrentCheckpoints(1);
        cpConfig.setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        // =============================================================
        // 3. Source：根据触发类型选择 TDEngine JDBC 轮询 或 MQTT 实时
        // =============================================================
        DataStream<DeviceDataEvent> source;

        // 水位线策略：允许 5 秒乱序
        WatermarkStrategy<DeviceDataEvent> watermarkStrategy =
                WatermarkStrategy.<DeviceDataEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, recordTimestamp) -> event.getTimestamp());

        if ("realtime".equalsIgnoreCase(config.getTriggerType())
                && config.getMqttConfig() != null
                && config.getMqttConfig().getBrokerUrl() != null) {
            // 实时模式：MQTT Source
            source = env.addSource(new MqttRealtimeSource(config))
                    .name("MQTT-Source")
                    .assignTimestampsAndWatermarks(watermarkStrategy);
        } else {
            // 定时模式（默认）：TDEngine JDBC 轮询 Source
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
            // 按设备分组（算法可同时处理同一设备的多个测点）
            keyed = source.keyBy(DeviceDataEvent::getDeviceId);
        } else {
            // 默认：按 deviceId#pointCode 分组（每个测点独立窗口）
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

        // Sink-1：TDEngine 结果回写
        if (config.getTdengineConfig() != null && config.getTdengineConfig().getJdbcUrl() != null) {
            result.addSink(new TDEngineResultSink(config.getTdengineConfig()))
                    .name("TDEngine-Result-Sink");
        }

        // Sink-2：MySQL 执行日志
        if (config.getMysqlConfig() != null && config.getMysqlConfig().getJdbcUrl() != null) {
            result.addSink(new MySQLExecutionLogSink(config.getMysqlConfig()))
                    .name("MySQL-Log-Sink");
        }

        // 兜底：打印到日志（调试用）
        result.print().name("Debug-Print-Sink");

        // =============================================================
        // 7. 启动 Job
        // =============================================================
        String jobName = "RuleJob-" + config.getRuleId() + "-" + config.getRuleName();
        env.execute(jobName);
    }
}
