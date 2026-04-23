package com.bit.iot.integration.tdengine;

import com.bit.iot.integration.config.TDEngineProperties;
import com.bit.iot.integration.model.dto.TimeSeriesPointDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HexFormat;

@Slf4j
@Component
public class TDEnginePointWriter {

    private static final int MAX_TABLE_NAME_LENGTH = 160;

    private final TDEngineProperties properties;

    public TDEnginePointWriter(TDEngineProperties properties) {
        this.properties = properties;
    }

    public int write(List<TimeSeriesPointDTO> points) {
        if (points == null || points.isEmpty()) {
            return 0;
        }
        validateConfig();
        try {
            Class.forName("com.taosdata.jdbc.rs.RestfulDriver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到 TDengine REST JDBC 驱动", e);
        }

        Map<ChildTableKey, List<TimeSeriesPointDTO>> groupedPoints = new LinkedHashMap<>();
        for (TimeSeriesPointDTO point : points) {
            validatePoint(point);
            ChildTableKey key = new ChildTableKey(
                    buildChildTableName(point.deviceId(), point.pointCode()),
                    point.deviceId(),
                    point.pointCode()
            );
            groupedPoints.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(point);
        }

        int written = 0;
        try (Connection connection = DriverManager.getConnection(
                properties.getJdbcUrl(),
                properties.getUsername(),
                properties.getPassword())) {
            ensureStable(connection);
            for (Map.Entry<ChildTableKey, List<TimeSeriesPointDTO>> entry : groupedPoints.entrySet()) {
                ensureChildTable(connection, entry.getKey());
                written += insertPoints(connection, entry.getKey().tableName(), entry.getValue());
            }
            return written;
        } catch (Exception e) {
            throw new IllegalStateException("写入 TDengine 时序数据失败: " + e.getMessage(), e);
        }
    }

    public String buildChildTableName(String deviceId, String pointCode) {
        String baseName = sanitizeIdentifier(properties.getSuperTable());
        String rawName = baseName + "_" + sanitizeIdentifier(deviceId) + "_" + sanitizeIdentifier(pointCode);
        if (rawName.length() <= MAX_TABLE_NAME_LENGTH) {
            return rawName;
        }
        return baseName + "_" + sha1(deviceId + "#" + pointCode);
    }

    private void ensureStable(Connection connection) throws Exception {
        String stable = quoteIdentifier(properties.getSuperTable());
        String sql = "CREATE STABLE IF NOT EXISTS " + stable
                + " (ts TIMESTAMP, `value` DOUBLE, quality TINYINT)"
                + " TAGS (device_id NCHAR(64), point_code NCHAR(64))";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void ensureChildTable(Connection connection, ChildTableKey key) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(key.tableName())
                + " USING " + quoteIdentifier(properties.getSuperTable())
                + " TAGS ('" + escapeSqlLiteral(key.deviceId()) + "', '" + escapeSqlLiteral(key.pointCode()) + "')";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private int insertPoints(Connection connection, String tableName, List<TimeSeriesPointDTO> points) throws Exception {
        String sql = "INSERT INTO " + quoteIdentifier(tableName) + " VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (TimeSeriesPointDTO point : points) {
                statement.setTimestamp(1, Timestamp.from(point.timestamp()));
                statement.setDouble(2, point.value());
                statement.setInt(3, point.quality() == null ? 0 : point.quality());
                statement.addBatch();
            }
            int[] results = statement.executeBatch();
            int count = 0;
            for (int result : results) {
                count += Math.max(result, 0);
            }
            return count == 0 ? points.size() : count;
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getJdbcUrl())) {
            throw new IllegalStateException("未配置 tdengine.jdbc-url");
        }
        if (!StringUtils.hasText(properties.getSuperTable())) {
            throw new IllegalStateException("未配置 tdengine.super-table");
        }
    }

    private void validatePoint(TimeSeriesPointDTO point) {
        if (point == null) {
            throw new IllegalArgumentException("时序点位不能为空");
        }
        if (!StringUtils.hasText(point.deviceId())) {
            throw new IllegalArgumentException("时序点位缺少 deviceId");
        }
        if (!StringUtils.hasText(point.pointCode())) {
            throw new IllegalArgumentException("时序点位缺少 pointCode");
        }
        if (point.timestamp() == null) {
            throw new IllegalArgumentException("时序点位缺少 timestamp");
        }
        if (point.value() == null) {
            throw new IllegalArgumentException("时序点位缺少 value");
        }
    }

    private String quoteIdentifier(String identifier) {
        return "`" + sanitizeIdentifier(identifier) + "`";
    }

    private String sanitizeIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String sanitized = value.trim().replaceAll("[^A-Za-z0-9_]", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        if (sanitized.isBlank()) {
            return "unknown";
        }
        if (Character.isDigit(sanitized.charAt(0))) {
            return "t_" + sanitized;
        }
        return sanitized;
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "''");
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 24);
        } catch (Exception e) {
            log.warn("生成 TDengine 子表哈希失败，使用 hashCode 兜底", e);
            return Integer.toHexString(value.hashCode());
        }
    }

    private record ChildTableKey(String tableName, String deviceId, String pointCode) {
    }
}
