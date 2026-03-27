package com.bit.iot.flink.job.sink;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.flink.job.model.AlgorithmOutputEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
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
    public void open(Configuration parameters) throws Exception {
        if (mysqlConfig != null && mysqlConfig.getJdbcUrl() != null) {
            connection = DriverManager.getConnection(
                    mysqlConfig.getJdbcUrl(), mysqlConfig.getUsername(), mysqlConfig.getPassword());
            LOG.info("MySQLExecutionLogSink 连接已建立");
        }
    }

    @Override
    public void invoke(AlgorithmOutputEvent event, Context context) {
        if (connection == null) return;

        String sql = "INSERT INTO rule_execution_log "
                + "(id, rule_id, start_time, end_time, exec_status, result_data, error_msg, duration_ms) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
            ps.setString(2, event.getRuleId());
            ps.setTimestamp(3, new Timestamp(event.getWindowStart()));
            ps.setTimestamp(4, new Timestamp(event.getWindowEnd()));
            ps.setInt(5, event.isSuccess() ? 1 : 2);

            if (event.getResultData() != null) {
                ps.setString(6, objectMapper.writeValueAsString(event.getResultData()));
            } else {
                ps.setNull(6, Types.VARCHAR);
            }

            ps.setString(7, event.getErrorMsg());
            ps.setLong(8, event.getDurationMs());

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
