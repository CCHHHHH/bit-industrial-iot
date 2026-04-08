package com.bit.iot.common.flink.connector.source;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.common.flink.connector.model.DeviceDataEvent;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.legacy.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TDEngine JDBC 轮询 Source
 * <p>
 * 定时从 TDEngine 拉取增量数据。
 * 每个 Source 并行实例负责一批设备的数据读取，通过 subtaskIndex 分片。
 * 支持 Checkpoint 恢复（保存 lastTimestamp 游标）。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class TDEngineJdbcSource extends RichParallelSourceFunction<DeviceDataEvent>
        implements CheckpointedFunction {

    private static final Logger LOG = LoggerFactory.getLogger(TDEngineJdbcSource.class);

    private final RuleJobConfig config;
    private final long pollIntervalMs;

    private transient volatile boolean isRunning = true;
    private transient Connection connection;

    /** 当前并行实例分配到的设备子集 */
    private transient List<RuleJobConfig.DataSourceConfig> myDevices;

    /** 增量游标：按设备维护 */
    private final Map<String, Long> lastTimestampByDevice = new HashMap<>();

    /** Checkpoint 状态 */
    private transient ListState<DeviceCursorState> checkpointedState;

    private long reconnectBackoffMs = 1000L;

    public TDEngineJdbcSource(RuleJobConfig config, long pollIntervalMs) {
        this.config = config;
        this.pollIntervalMs = pollIntervalMs;
    }

    // ================================================================
    // Lifecycle
    // ================================================================

    @Override
    public void open(OpenContext openContext) throws Exception {
        int subtaskIndex = getRuntimeContext().getTaskInfo().getIndexOfThisSubtask();
        int parallelism = getRuntimeContext().getTaskInfo().getNumberOfParallelSubtasks();

        List<RuleJobConfig.DataSourceConfig> allDevices = config.getDataSources();
        myDevices = new ArrayList<>();
        if (allDevices != null) {
            for (int i = 0; i < allDevices.size(); i++) {
                if (i % parallelism == subtaskIndex) {
                    myDevices.add(allDevices.get(i));
                }
            }
        }

        LOG.info("TDEngineJdbcSource[subtask-{}] 分配到 {} 个设备", subtaskIndex, myDevices.size());
        for (RuleJobConfig.DataSourceConfig device : myDevices) {
            lastTimestampByDevice.putIfAbsent(device.getDeviceId(), 0L);
        }
        ensureConnection();
    }

    @Override
    public void close() throws Exception {
        isRunning = false;
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // ================================================================
    // Source Logic
    // ================================================================

    @Override
    public void run(SourceContext<DeviceDataEvent> ctx) throws Exception {
        while (isRunning) {
            try {
                ensureConnection();
            } catch (Exception e) {
                LOG.warn("TDEngine 重连失败，{}ms 后重试: {}", reconnectBackoffMs, e.getMessage());
                Thread.sleep(reconnectBackoffMs);
                reconnectBackoffMs = Math.min(reconnectBackoffMs * 2, 30_000L);
                continue;
            }
            reconnectBackoffMs = 1000L;

            for (RuleJobConfig.DataSourceConfig ds : myDevices) {
                try {
                    pollDevice(ctx, ds);
                } catch (Exception e) {
                    LOG.error("拉取设备 {} 数据失败", ds.getDeviceId(), e);
                }
            }

            Thread.sleep(pollIntervalMs);
        }
    }

    private void pollDevice(SourceContext<DeviceDataEvent> ctx,
                            RuleJobConfig.DataSourceConfig ds) throws SQLException {
        RuleJobConfig.TDEngineConfig td = config.getTdengineConfig();
        String superTable = td.getSuperTable();
        long cursor = lastTimestampByDevice.getOrDefault(ds.getDeviceId(), 0L);
        long rangeStart = Math.max(cursor, resolveConfiguredBoundary(ds.getTimeRangeStart(), true));
        long rangeEnd = resolveConfiguredBoundary(ds.getTimeRangeEnd(), false);
        if (rangeEnd <= rangeStart) {
            return;
        }

        StringBuilder sql = new StringBuilder("SELECT ts, device_id, point_code, value, quality FROM ")
                .append(superTable)
                .append(" WHERE device_id = ? AND ts > ? AND ts <= ? ");
        List<Object> params = new ArrayList<>();
        params.add(ds.getDeviceId());
        params.add(new Timestamp(rangeStart));
        params.add(new Timestamp(rangeEnd));

        if (ds.getPointCodes() != null && !ds.getPointCodes().isEmpty()) {
            sql.append(" AND point_code IN (")
                    .append(String.join(",", java.util.Collections.nCopies(ds.getPointCodes().size(), "?")))
                    .append(")");
            params.addAll(ds.getPointCodes());
        }
        sql.append(" ORDER BY ts ASC LIMIT ?");
        params.add(10_000);

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long ts = rs.getTimestamp("ts").getTime();
                    DeviceDataEvent event = new DeviceDataEvent(
                            rs.getString("device_id"),
                            rs.getString("point_code"),
                            ts,
                            rs.getDouble("value"),
                            rs.getInt("quality")
                    );

                    synchronized (ctx.getCheckpointLock()) {
                        ctx.collectWithTimestamp(event, ts);
                        lastTimestampByDevice.put(ds.getDeviceId(), Math.max(cursor, ts));
                    }
                }
            }
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    // ================================================================
    // Checkpoint
    // ================================================================

    @Override
    public void snapshotState(FunctionSnapshotContext ctx) throws Exception {
        checkpointedState.clear();
        for (Map.Entry<String, Long> entry : lastTimestampByDevice.entrySet()) {
            checkpointedState.add(new DeviceCursorState(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext ctx) throws Exception {
        checkpointedState = ctx.getOperatorStateStore().getListState(
                new ListStateDescriptor<>("deviceCursorState", DeviceCursorState.class));

        if (ctx.isRestored()) {
            for (DeviceCursorState state : checkpointedState.get()) {
                lastTimestampByDevice.put(state.getDeviceId(), state.getLastTimestamp());
            }
            LOG.info("从 Checkpoint 恢复设备游标: {}", lastTimestampByDevice);
        }
    }

    private void ensureConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        RuleJobConfig.TDEngineConfig td = config.getTdengineConfig();
        if (td == null || td.getJdbcUrl() == null) {
            throw new SQLException("未配置 TDEngine JDBC");
        }
        connection = DriverManager.getConnection(td.getJdbcUrl(), td.getUsername(), td.getPassword());
    }

    private long resolveConfiguredBoundary(String configuredBoundary, boolean startBoundary) {
        if (configuredBoundary == null || configuredBoundary.isBlank()) {
            return startBoundary ? 0L : System.currentTimeMillis();
        }
        String value = configuredBoundary.trim();
        try {
            if (value.matches("^\\d{13}$")) {
                return Long.parseLong(value);
            }
            if (value.matches("^\\d{10}$")) {
                return Long.parseLong(value) * 1000L;
            }
            if (value.length() == 8) {
                LocalDate date = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
                return LocalDateTime.of(date, LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception e) {
            LOG.warn("忽略无法解析的时间范围配置: {}", configuredBoundary);
            return startBoundary ? 0L : System.currentTimeMillis();
        }
    }

    public static class DeviceCursorState implements Serializable {
        private static final long serialVersionUID = 1L;
        private String deviceId;
        private Long lastTimestamp;

        public DeviceCursorState() {
        }

        public DeviceCursorState(String deviceId, Long lastTimestamp) {
            this.deviceId = deviceId;
            this.lastTimestamp = lastTimestamp;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public Long getLastTimestamp() {
            return lastTimestamp;
        }

        public void setLastTimestamp(Long lastTimestamp) {
            this.lastTimestamp = lastTimestamp;
        }
    }
}
