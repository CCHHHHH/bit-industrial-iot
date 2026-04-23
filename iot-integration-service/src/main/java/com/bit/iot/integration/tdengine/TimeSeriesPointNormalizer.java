package com.bit.iot.integration.tdengine;

import com.bit.iot.integration.model.dto.TimeSeriesPointDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TimeSeriesPointNormalizer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    public TimeSeriesPointNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TimeSeriesPointDTO> normalize(Object rawResult, Instant fallbackTimestamp) {
        if (rawResult == null) {
            return List.of();
        }
        Object value = unwrapJson(rawResult);
        Instant defaultTimestamp = fallbackTimestamp == null ? Instant.now() : fallbackTimestamp;
        List<TimeSeriesPointDTO> result = new ArrayList<>();
        appendNormalized(value, defaultTimestamp, result);
        return result;
    }

    private Object unwrapJson(Object rawResult) {
        if (!(rawResult instanceof String text)) {
            return rawResult;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            throw new IllegalArgumentException("插件时序返回值必须是 JSON 对象或数组");
        }
        try {
            return objectMapper.readValue(trimmed, new TypeReference<Object>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("插件时序返回值 JSON 解析失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void appendNormalized(Object value, Instant fallbackTimestamp, List<TimeSeriesPointDTO> result) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                appendNormalized(unwrapJson(item), fallbackTimestamp, result);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                appendNormalized(Array.get(value, i), fallbackTimestamp, result);
            }
            return;
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException("插件时序返回值只支持对象、数组或 JSON 字符串");
        }
        Map<String, Object> map = toStringKeyMap(source);
        if (map.containsKey("points")) {
            appendNormalized(map.get("points"), fallbackTimestamp, result);
            return;
        }
        if (map.get("values") instanceof Map<?, ?> values) {
            appendSimulatorPayload(map, values, fallbackTimestamp, result);
            return;
        }
        TimeSeriesPointDTO point = toPoint(map, fallbackTimestamp);
        if (point != null) {
            result.add(point);
        }
    }

    private void appendSimulatorPayload(Map<String, Object> source,
                                        Map<?, ?> values,
                                        Instant fallbackTimestamp,
                                        List<TimeSeriesPointDTO> result) {
        String deviceId = stringValue(firstPresent(source, "deviceId", "device_id"));
        Instant timestamp = parseTimestamp(firstPresent(source, "timestamp", "ts", "time"), fallbackTimestamp);
        Integer defaultQuality = intValue(firstPresent(source, "quality", "qualityCode"), 0);
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("时序数据缺少 deviceId");
        }
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String pointCode = stringValue(entry.getKey());
            if (pointCode == null || pointCode.isBlank()) {
                continue;
            }
            Object rawValue = entry.getValue();
            Double pointValue;
            Integer quality = defaultQuality;
            if (rawValue instanceof Map<?, ?> nested) {
                Map<String, Object> nestedMap = toStringKeyMap(nested);
                pointValue = doubleValue(firstPresent(nestedMap, "value", "pointValue"));
                quality = intValue(firstPresent(nestedMap, "quality", "qualityCode"), defaultQuality);
            } else {
                pointValue = doubleValue(rawValue);
            }
            if (pointValue == null) {
                continue;
            }
            result.add(new TimeSeriesPointDTO(deviceId, pointCode, timestamp, pointValue, quality));
        }
    }

    private TimeSeriesPointDTO toPoint(Map<String, Object> source, Instant fallbackTimestamp) {
        String deviceId = stringValue(firstPresent(source, "deviceId", "device_id"));
        String pointCode = stringValue(firstPresent(source, "pointCode", "point_code"));
        Double value = doubleValue(firstPresent(source, "value", "pointValue", "point_value"));
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("时序数据缺少 deviceId");
        }
        if (pointCode == null || pointCode.isBlank()) {
            throw new IllegalArgumentException("时序数据缺少 pointCode");
        }
        if (value == null) {
            throw new IllegalArgumentException("时序数据缺少数值 value");
        }
        Instant timestamp = parseTimestamp(firstPresent(source, "timestamp", "ts", "time"), fallbackTimestamp);
        Integer quality = intValue(firstPresent(source, "quality", "qualityCode"), 0);
        return new TimeSeriesPointDTO(deviceId, pointCode, timestamp, value, quality);
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private Object firstPresent(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                return source.get(key);
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double doubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Instant parseTimestamp(Object value, Instant fallbackTimestamp) {
        if (value == null) {
            return fallbackTimestamp;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return fallbackTimestamp;
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(text));
        } catch (NumberFormatException ignored) {
            // Continue trying formatted timestamps.
        }
        try {
            return Instant.parse(text);
        } catch (Exception ignored) {
            // Continue trying local date-time.
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("非法时序时间戳: " + text, e);
        }
    }
}
