package com.bit.iot.data.service.impl;

import bit.iot.common.controller.BusinessException;
import com.bit.iot.data.config.TDEngineProperties;
import com.bit.iot.data.model.request.RawTimeSeriesQueryRequest;
import com.bit.iot.data.model.request.RuleDataSourceQuery;
import com.bit.iot.data.model.request.RuleWindowQueryRequest;
import com.bit.iot.data.model.vo.TimeSeriesPointVO;
import com.bit.iot.data.service.TimeSeriesQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TimeSeriesQueryServiceImpl implements TimeSeriesQueryService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TDEngineProperties tdEngineProperties;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public TimeSeriesQueryServiceImpl(TDEngineProperties tdEngineProperties) {
        this.tdEngineProperties = tdEngineProperties;
    }

    @Override
    public List<TimeSeriesPointVO> queryRaw(RawTimeSeriesQueryRequest request) {
        validateRawRequest(request);
        int limit = normalizeLimit(request.getLimit());

        StringBuilder sql = new StringBuilder()
                .append("SELECT ts, device_id, point_code, `value`, quality FROM ")
                .append(resolveTableName())
                .append(" WHERE ts >= ? AND ts <= ? ");

        List<Object> params = new ArrayList<>();
        params.add(new Timestamp(request.getStartTime()));
        params.add(new Timestamp(request.getEndTime()));

        if (request.getDeviceIds() != null && !request.getDeviceIds().isEmpty()) {
            sql.append(" AND device_id IN (").append(placeholders(request.getDeviceIds().size())).append(")");
            params.addAll(request.getDeviceIds());
        }
        if (request.getPointCodes() != null && !request.getPointCodes().isEmpty()) {
            sql.append(" AND point_code IN (").append(placeholders(request.getPointCodes().size())).append(")");
            params.addAll(request.getPointCodes());
        }

        sql.append(" ORDER BY ts ASC LIMIT ?");
        params.add(limit);
        return executeQuery(sql.toString(), params);
    }

    @Override
    public List<TimeSeriesPointVO> queryRuleWindow(RuleWindowQueryRequest request) {
        validateRuleWindowRequest(request);

        long fallbackStart = request.getQueryStartTime();
        long fallbackEnd = request.getQueryEndTime();
        int limitPerSource = normalizeLimit(request.getLimitPerSource());
        LinkedHashSet<TimeSeriesPointVO> merged = new LinkedHashSet<>();

        for (RuleDataSourceQuery dataSource : request.getDataSources()) {
            if (!StringUtils.hasText(dataSource.getDeviceId())) {
                continue;
            }
            long start = resolveSourceBoundary(dataSource.getTimeRangeStart(), fallbackStart, fallbackEnd, true);
            long end = resolveSourceBoundary(dataSource.getTimeRangeEnd(), fallbackStart, fallbackEnd, false);
            if (end < start) {
                log.warn("跳过非法时间范围的数据源: deviceId={}, start={}, end={}",
                        dataSource.getDeviceId(), start, end);
                continue;
            }

            StringBuilder sql = new StringBuilder()
                    .append("SELECT ts, device_id, point_code, `value`, quality FROM ")
                    .append(resolveTableName())
                    .append(" WHERE device_id = ? AND ts >= ? AND ts <= ? ");
            List<Object> params = new ArrayList<>();
            params.add(dataSource.getDeviceId());
            params.add(new Timestamp(start));
            params.add(new Timestamp(end));

            if (dataSource.getPointCodes() != null && !dataSource.getPointCodes().isEmpty()) {
                sql.append(" AND point_code IN (").append(placeholders(dataSource.getPointCodes().size())).append(")");
                params.addAll(dataSource.getPointCodes());
            }

            sql.append(" ORDER BY ts ASC LIMIT ?");
            params.add(limitPerSource);
            merged.addAll(executeQuery(sql.toString(), params));
        }

        return merged.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TimeSeriesPointVO::getTimestamp)
                        .thenComparing(TimeSeriesPointVO::getDeviceId)
                        .thenComparing(TimeSeriesPointVO::getPointCode))
                .toList();
    }

    @Override
    public Map<String, Object> health() {
        long start = System.currentTimeMillis();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            boolean ok = resultSet.next();
            Map<String, Object> result = new HashMap<>();
            result.put("status", ok ? "UP" : "DOWN");
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("jdbcUrl", tdEngineProperties.getJdbcUrl());
            return result;
        } catch (Exception e) {
            log.error("TDEngine 健康检查失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "DOWN");
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("error", e.getMessage());
            return result;
        }
    }

    public long resolveSourceBoundary(String configuredValue, long fallbackStart, long fallbackEnd, boolean startBoundary) {
        if (!StringUtils.hasText(configuredValue)) {
            return startBoundary ? fallbackStart : fallbackEnd;
        }

        String value = configuredValue.trim();
        try {
            if (value.matches("^\\d{13}$")) {
                return Long.parseLong(value);
            }
            if (value.matches("^\\d{10}$")) {
                return Long.parseLong(value) * 1000L;
            }
            if (value.length() == 8) {
                LocalDate date = Instant.ofEpochMilli(fallbackEnd).atZone(zoneId).toLocalDate();
                LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.parse(value, TIME_FORMATTER));
                return dateTime.atZone(zoneId).toInstant().toEpochMilli();
            }
            try {
                return Instant.parse(value).toEpochMilli();
            } catch (DateTimeParseException ignored) {
                LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME_FORMATTER);
                return dateTime.atZone(zoneId).toInstant().toEpochMilli();
            }
        } catch (Exception e) {
            throw new BusinessException("非法时间配置: " + configuredValue);
        }
    }

    private List<TimeSeriesPointVO> executeQuery(String sql, List<Object> params) {
        log.debug("执行时序查询: sql={}, params={}", sql, params);
        List<TimeSeriesPointVO> results = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(tdEngineProperties.getQueryTimeoutSeconds());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new TimeSeriesPointVO(
                            resultSet.getString("device_id"),
                            resultSet.getString("point_code"),
                            resultSet.getTimestamp("ts").getTime(),
                            resultSet.getDouble("value"),
                            resultSet.getInt("quality")
                    ));
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行时序查询失败", e);
            throw new BusinessException("时序查询失败: " + e.getMessage());
        }
        return results;
    }

    private Connection openConnection() throws Exception {
        if (!StringUtils.hasText(tdEngineProperties.getJdbcUrl())) {
            throw new BusinessException("未配置 tdengine.jdbc-url");
        }
        return DriverManager.getConnection(
                tdEngineProperties.getJdbcUrl(),
                tdEngineProperties.getUsername(),
                tdEngineProperties.getPassword()
        );
    }

    private void validateRawRequest(RawTimeSeriesQueryRequest request) {
        if (request == null) {
            throw new BusinessException("请求不能为空");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException("startTime 和 endTime 不能为空");
        }
        if (request.getEndTime() < request.getStartTime()) {
            throw new BusinessException("endTime 不能早于 startTime");
        }
        if ((request.getDeviceIds() == null || request.getDeviceIds().isEmpty())
                && (request.getPointCodes() == null || request.getPointCodes().isEmpty())) {
            throw new BusinessException("deviceIds 和 pointCodes 不能同时为空");
        }
    }

    private void validateRuleWindowRequest(RuleWindowQueryRequest request) {
        if (request == null) {
            throw new BusinessException("请求不能为空");
        }
        if (request.getQueryStartTime() == null || request.getQueryEndTime() == null) {
            throw new BusinessException("queryStartTime 和 queryEndTime 不能为空");
        }
        if (request.getQueryEndTime() < request.getQueryStartTime()) {
            throw new BusinessException("queryEndTime 不能早于 queryStartTime");
        }
        if (request.getDataSources() == null || request.getDataSources().isEmpty()) {
            throw new BusinessException("dataSources 不能为空");
        }
    }

    private int normalizeLimit(Integer requestedLimit) {
        int defaultLimit = 1000;
        int maxLimit = tdEngineProperties.getMaxLimitPerQuery();
        if (requestedLimit == null || requestedLimit <= 0) {
            return Math.min(defaultLimit, maxLimit);
        }
        return Math.min(requestedLimit, maxLimit);
    }

    private String resolveTableName() {
        String tableName = tdEngineProperties.getSuperTable();
        if (!StringUtils.hasText(tableName) || !IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            throw new BusinessException("非法的 tdengine.super-table 配置");
        }
        return tableName;
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }
}
