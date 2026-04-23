package com.bit.iot.data.service.impl;

import com.bit.iot.data.config.TDEngineProperties;
import com.bit.iot.data.model.request.RawTimeSeriesQueryRequest;
import com.bit.iot.data.model.vo.TimeSeriesPointVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

class TimeSeriesQueryServiceTdengineIT {

    private static final String JDBC_URL = "jdbc:TAOS-RS://127.0.0.1:6041/iot_data";
    private static final String ROOT_JDBC_URL = "jdbc:TAOS-RS://127.0.0.1:6041";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "taosdata";

    @Test
    void shouldQueryPointDataThroughRestJdbc() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("tdengine.it.enabled"),
                "Set -Dtdengine.it.enabled=true to run local TDengine integration test");
        Assumptions.assumeTrue(isTdengineRestAvailable(), "TDengine REST 6041 is not available");

        initializePointData();

        TDEngineProperties properties = new TDEngineProperties();
        properties.setJdbcUrl(JDBC_URL);
        properties.setUsername(USERNAME);
        properties.setPassword(PASSWORD);
        properties.setSuperTable("point_data");
        properties.setQueryTimeoutSeconds(30);
        properties.setMaxLimitPerQuery(100);

        TimeSeriesQueryServiceImpl service = new TimeSeriesQueryServiceImpl(properties);
        Assertions.assertEquals("UP", service.health().get("status"));

        long now = System.currentTimeMillis();
        RawTimeSeriesQueryRequest request = new RawTimeSeriesQueryRequest();
        request.setDeviceIds(List.of("test-device-001"));
        request.setPointCodes(List.of("TEMP_001"));
        request.setStartTime(now - 5 * 60 * 1000L);
        request.setEndTime(now + 5 * 60 * 1000L);
        request.setLimit(10);

        List<TimeSeriesPointVO> points = service.queryRaw(request);
        Assertions.assertFalse(points.isEmpty());

        TimeSeriesPointVO point = points.getLast();
        Assertions.assertEquals("test-device-001", point.getDeviceId());
        Assertions.assertEquals("TEMP_001", point.getPointCode());
        Assertions.assertEquals(25.5D, point.getValue(), 0.0001D);
        Assertions.assertEquals(0, point.getQuality());
    }

    private boolean isTdengineRestAvailable() {
        try {
            Class.forName("com.taosdata.jdbc.rs.RestfulDriver");
            try (Connection connection = DriverManager.getConnection(ROOT_JDBC_URL, USERNAME, PASSWORD);
                 Statement statement = connection.createStatement()) {
                statement.executeQuery("SHOW DATABASES");
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void initializePointData() throws Exception {
        String sql = new ClassPathResource("sql/tdengine_point_data.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = DriverManager.getConnection(ROOT_JDBC_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            Arrays.stream(sql.split(";"))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .forEach(item -> execute(statement, item));
        }
    }

    private void execute(Statement statement, String sql) {
        try {
            statement.execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException("执行 TDengine SQL 失败: " + sql, e);
        }
    }
}
