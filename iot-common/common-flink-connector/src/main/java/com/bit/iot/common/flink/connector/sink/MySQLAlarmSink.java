package com.bit.iot.common.flink.connector.sink;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.common.flink.alarm.AlarmSupport;
import com.bit.iot.common.flink.connector.model.AlgorithmOutputEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.sink.legacy.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MySQL 告警 Sink。
 */
public class MySQLAlarmSink extends RichSinkFunction<AlgorithmOutputEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MySQLAlarmSink.class);

    private final RuleJobConfig jobConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private transient Connection connection;
    private transient Map<String, String> deviceNameMap;

    public MySQLAlarmSink(RuleJobConfig jobConfig) {
        this.jobConfig = jobConfig;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        RuleJobConfig.MysqlConfig mysqlConfig = jobConfig == null ? null : jobConfig.getMysqlConfig();
        if (mysqlConfig != null && mysqlConfig.getJdbcUrl() != null) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    mysqlConfig.getJdbcUrl(),
                    mysqlConfig.getUsername(),
                    mysqlConfig.getPassword());
            LOG.info("MySQLAlarmSink 连接已建立");
        }
        deviceNameMap = new HashMap<>();
        if (jobConfig != null && jobConfig.getDataSources() != null) {
            for (RuleJobConfig.DataSourceConfig dataSource : jobConfig.getDataSources()) {
                if (dataSource.getDeviceId() != null && dataSource.getDeviceName() != null) {
                    deviceNameMap.putIfAbsent(dataSource.getDeviceId(), dataSource.getDeviceName());
                }
            }
        }
    }

    @Override
    public void invoke(AlgorithmOutputEvent event, Context context) {
        if (connection == null || event == null || !event.isSuccess()) {
            return;
        }
        Map<String, Object> resultData = event.getResultData();
        if (!AlarmSupport.isAlert(resultData)) {
            return;
        }

        AlarmSupport.AlarmKey alarmKey = AlarmSupport.parseWindowKey(event.getKey());
        String deviceId = alarmKey.deviceId();
        String pointCode = alarmKey.pointCode();
        String deviceName = deviceId == null ? null : deviceNameMap.get(deviceId);
        String message = AlarmSupport.resolveMessage(resultData, jobConfig.getRuleName());
        String level = AlarmSupport.resolveLevel(resultData);
        String metricName = AlarmSupport.resolveMetricName(resultData);
        String metricValue = AlarmSupport.resolveMetricValue(resultData);
        long triggerMillis = event.getProcessTime() > 0 ? event.getProcessTime() : event.getWindowEnd();
        Timestamp triggerTime = new Timestamp(triggerMillis > 0 ? triggerMillis : System.currentTimeMillis());
        String dedupKey = buildDedupKey(event.getRuleId(), deviceId, pointCode);

        String sql = "INSERT INTO alarm_record "
                + "(id, source_type, source_id, rule_id, rule_name, device_id, device_name, point_code, "
                + "dedup_key, alarm_title, alarm_message, alarm_level, alarm_status, trigger_count, "
                + "first_trigger_time, last_trigger_time, resolved_time, metric_name, metric_value, "
                + "result_data, create_time, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', 1, ?, ?, NULL, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "rule_name = VALUES(rule_name), "
                + "device_name = COALESCE(VALUES(device_name), device_name), "
                + "alarm_title = VALUES(alarm_title), "
                + "alarm_message = VALUES(alarm_message), "
                + "alarm_level = VALUES(alarm_level), "
                + "trigger_count = trigger_count + 1, "
                + "last_trigger_time = VALUES(last_trigger_time), "
                + "metric_name = VALUES(metric_name), "
                + "metric_value = VALUES(metric_value), "
                + "result_data = VALUES(result_data), "
                + "update_time = NOW()";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
            ps.setString(2, "rule");
            ps.setString(3, event.getRuleId());
            ps.setString(4, event.getRuleId());
            ps.setString(5, jobConfig.getRuleName());
            ps.setString(6, deviceId);
            ps.setString(7, deviceName);
            ps.setString(8, pointCode);
            ps.setString(9, dedupKey);
            ps.setString(10, message);
            ps.setString(11, message);
            ps.setString(12, level);
            ps.setTimestamp(13, triggerTime);
            ps.setTimestamp(14, triggerTime);
            setNullable(ps, 15, metricName);
            setNullable(ps, 16, metricValue);
            ps.setString(17, objectMapper.writeValueAsString(resultData));
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.error("写入 MySQL 告警失败: ruleId={}, key={}", event.getRuleId(), event.getKey(), e);
        }
    }

    private String buildDedupKey(String ruleId, String deviceId, String pointCode) {
        return String.join(":",
                "rule",
                ruleId == null ? "" : ruleId,
                deviceId == null ? "" : deviceId,
                pointCode == null ? "" : pointCode);
    }

    private void setNullable(PreparedStatement ps, int parameterIndex, String value) throws Exception {
        if (value == null || value.isBlank()) {
            ps.setNull(parameterIndex, Types.VARCHAR);
            return;
        }
        ps.setString(parameterIndex, value);
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
