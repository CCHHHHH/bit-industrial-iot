package com.bit.iot.common.flink.connector.sink;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.common.flink.connector.model.AlgorithmOutputEvent;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.sink.legacy.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;

/**
 * TDEngine 结果回写 Sink
 * <p>
 * 将算法计算结果写入 TDEngine 结果超级表。
 * 表结构：ts TIMESTAMP, rule_id NCHAR(32), key NCHAR(100), metric_name NCHAR(100), metric_value DOUBLE
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class TDEngineResultSink extends RichSinkFunction<AlgorithmOutputEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(TDEngineResultSink.class);

    private final RuleJobConfig.TDEngineConfig tdConfig;
    private transient Connection connection;
    private transient boolean stableReady;
    private transient String resultTableName;

    public TDEngineResultSink(RuleJobConfig.TDEngineConfig tdConfig) {
        this.tdConfig = tdConfig;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
//        if (tdConfig != null && tdConfig.getJdbcUrl() != null) {
//            connection = DriverManager.getConnection(
//                    tdConfig.getJdbcUrl(), tdConfig.getUsername(), tdConfig.getPassword());
//            LOG.info("TDEngineResultSink 连接已建立");
//        }
    }

    @Override
    public void invoke(AlgorithmOutputEvent event, Context context) {
        if (connection == null || !event.isSuccess() || event.getResultData() == null) {
            return;
        }

        try {
            ensureStableReady(event.getRuleId());
            Map<String, Object> resultData = event.getResultData();
            for (Map.Entry<String, Object> entry : resultData.entrySet()) {
                double metricValue;
                try {
                    metricValue = Double.parseDouble(String.valueOf(entry.getValue()));
                } catch (NumberFormatException e) {
                    continue; // 跳过非数值类型
                }

                String sql = "INSERT INTO " + resultTableName
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setTimestamp(1, new Timestamp(event.getProcessTime()));
                    ps.setString(2, event.getKey());
                    ps.setString(3, entry.getKey());
                    ps.setDouble(4, metricValue);
                    ps.setLong(5, event.getProcessTime());
                    ps.setLong(6, event.getWindowStart());
                    ps.setLong(7, event.getWindowEnd());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOG.error("写入 TDEngine 结果失败: ruleId={}, key={}", event.getRuleId(), event.getKey(), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void ensureStableReady(String ruleId) throws Exception {
        if (stableReady || connection == null) {
            return;
        }
        String stableName = tdConfig.getResultStable() == null || tdConfig.getResultStable().isBlank()
                ? "rule_result_stable"
                : tdConfig.getResultStable();
        resultTableName = stableName + "_" + ruleId.replace("-", "_");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE STABLE IF NOT EXISTS " + stableName
                    + " (ts TIMESTAMP, window_key NCHAR(128), metric_name NCHAR(128), metric_value DOUBLE, "
                    + "process_time BIGINT, window_start BIGINT, window_end BIGINT) TAGS (rule_id NCHAR(64))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + resultTableName
                    + " USING " + stableName + " TAGS ('" + ruleId + "')");
        }
        stableReady = true;
    }
}
