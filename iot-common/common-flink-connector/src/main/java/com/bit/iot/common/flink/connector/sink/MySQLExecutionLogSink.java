package com.bit.iot.common.flink.connector.sink;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.common.flink.connector.model.AlgorithmOutputEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.sink.legacy.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.UUID;

/**
 * MySQL 执行日志 Sink
 * <p>
 * 将每个窗口的算法执行结果写入 MySQL rule_execution_log 表，
 * 供 iot-rule-service 管理界面查询。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class MySQLExecutionLogSink extends RichSinkFunction<AlgorithmOutputEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MySQLExecutionLogSink.class);

    private final RuleJobConfig.MysqlConfig mysqlConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private transient Connection connection;

    public MySQLExecutionLogSink(RuleJobConfig.MysqlConfig mysqlConfig) {
        this.mysqlConfig = mysqlConfig;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        if (mysqlConfig != null && mysqlConfig.getJdbcUrl() != null) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    mysqlConfig.getJdbcUrl(), mysqlConfig.getUsername(), mysqlConfig.getPassword());
            LOG.info("MySQLExecutionLogSink 连接已建立");
        }
    }

    @Override
    public void invoke(AlgorithmOutputEvent event, Context context) {
        if (connection == null) return;

        String sql = "INSERT INTO rule_execution_log "
                + "(id, rule_id, window_key, start_time, end_time, exec_status, result_data, error_msg, duration_ms) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE exec_status = VALUES(exec_status), "
                + "result_data = VALUES(result_data), error_msg = VALUES(error_msg), duration_ms = VALUES(duration_ms)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
            ps.setString(2, event.getRuleId());
            ps.setString(3, event.getKey());
            ps.setTimestamp(4, new Timestamp(event.getWindowStart()));
            ps.setTimestamp(5, new Timestamp(event.getWindowEnd()));
            ps.setInt(6, event.isSuccess() ? 1 : 2);

            if (event.getResultData() != null) {
                ps.setString(7, objectMapper.writeValueAsString(event.getResultData()));
            } else {
                ps.setNull(7, Types.VARCHAR);
            }

            ps.setString(8, event.getErrorMsg());
            ps.setLong(9, event.getDurationMs());

            ps.executeUpdate();
        } catch (Exception e) {
            LOG.error("写入 MySQL 执行日志失败: ruleId={}", event.getRuleId(), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
