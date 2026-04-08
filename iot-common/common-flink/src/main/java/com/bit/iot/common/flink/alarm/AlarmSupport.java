package com.bit.iot.common.flink.alarm;

import java.util.Locale;
import java.util.Map;

/**
 * 告警判定与字段解析工具。
 */
public final class AlarmSupport {

    private AlarmSupport() {
    }

    public static boolean isAlert(Map<String, Object> resultData) {
        if (resultData == null || resultData.isEmpty()) {
            return false;
        }
        return parseBoolean(resultData.get("alert"));
    }

    public static AlarmKey parseWindowKey(String key) {
        if (key == null || key.isBlank()) {
            return new AlarmKey(null, null, null);
        }
        int idx = key.indexOf('#');
        if (idx < 0) {
            return new AlarmKey(key, key, null);
        }
        String deviceId = key.substring(0, idx);
        String pointCode = idx + 1 < key.length() ? key.substring(idx + 1) : null;
        return new AlarmKey(key, deviceId, pointCode);
    }

    public static String resolveLevel(Map<String, Object> resultData) {
        String raw = asText(resultData == null ? null : resultData.get("alertLevel"));
        if (raw == null || raw.isBlank()) {
            return "warning";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("info".equals(normalized) || "warning".equals(normalized) || "error".equals(normalized)) {
            return normalized;
        }
        if ("warn".equals(normalized)) {
            return "warning";
        }
        if ("err".equals(normalized)) {
            return "error";
        }
        return "warning";
    }

    public static String resolveMessage(Map<String, Object> resultData, String ruleName) {
        String message = asText(resultData == null ? null : resultData.get("alertMessage"));
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        String fallback = ruleName == null || ruleName.isBlank() ? "规则" : ruleName.trim();
        return fallback + "触发告警";
    }

    public static String resolveMetricName(Map<String, Object> resultData) {
        return trimToNull(asText(resultData == null ? null : resultData.get("metricName")));
    }

    public static String resolveMetricValue(Map<String, Object> resultData) {
        return trimToNull(asText(resultData == null ? null : resultData.get("metricValue")));
    }

    private static boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim().toLowerCase(Locale.ROOT);
            return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
        }
        return false;
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class AlarmKey {
        private final String rawKey;
        private final String deviceId;
        private final String pointCode;

        public AlarmKey(String rawKey, String deviceId, String pointCode) {
            this.rawKey = rawKey;
            this.deviceId = deviceId;
            this.pointCode = pointCode;
        }

        public String rawKey() {
            return rawKey;
        }

        public String deviceId() {
            return deviceId;
        }

        public String pointCode() {
            return pointCode;
        }
    }
}
