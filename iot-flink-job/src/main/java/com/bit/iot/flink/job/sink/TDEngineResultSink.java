package com.bit.iot.flink.job.sink;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.flink.job.model.AlgorithmOutputEvent;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

    public TDEngineResultSink(RuleJobConfig.TDEngineConfig tdConfig) {
        this.tdConfig = tdConfig;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        if (tdConfig != null && tdConfig.getJdbcUrl() != null) {
            connection = DriverManager.getConnection(
                    tdConfig.getJdbcUrl(), tdConfig.getUsername(), tdConfig.getPassword());
            LOG.info("TDEngineResultSink 连接已建立");
        }
    }

    @Override
    public void invoke(AlgorithmOutputEvent event, Context context) {
        if (connection == null || !event.isSuccess() || event.getResultData() == null) {
            return;
        }

        try {
            Map<String, Object> resultData = event.getResultData();
            for (Map.Entry<String, Object> entry : resultData.entrySet()) {
                double metricValue;
                try {
                    metricValue = Double.parseDouble(String.valueOf(entry.getValue()));
                } catch (NumberFormatException e) {
                    continue; // 跳过非数值类型
                }

                // 使用自动建表语法
                String sql = String.format(
                        "INSERT INTO rule_result_%s USING rule_result_stable TAGS ('%s') VALUES (?, ?, ?, ?, ?)",
                        event.getRuleId().replace("-", ""), event.getRuleId()
                );

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setTimestamp(1, new Timestamp(event.getProcessTime()));
                    ps.setString(2, event.getRuleId());
                    ps.setString(3, event.getKey());
                    ps.setString(4, entry.getKey());
                    ps.setDouble(5, metricValue);
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
}
