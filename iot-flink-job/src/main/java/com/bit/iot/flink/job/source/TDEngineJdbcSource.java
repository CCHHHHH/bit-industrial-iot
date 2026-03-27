package com.bit.iot.flink.job.source;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.flink.job.model.DeviceDataEvent;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /** 增量游标：上一次拉取的最大时间戳 */
    private volatile long lastTimestamp = 0L;

    /** Checkpoint 状态 */
    private transient ListState<Long> checkpointedState;

    public TDEngineJdbcSource(RuleJobConfig config, long pollIntervalMs) {
        this.config = config;
        this.pollIntervalMs = pollIntervalMs;
    }

    // ================================================================
    // Lifecycle
    // ================================================================

    @Override
    public void open(Configuration parameters) throws Exception {
        // 按 subtaskIndex 分片设备
        int subtaskIndex = getRuntimeContext().getIndexOfThisSubtask();
        int parallelism = getRuntimeContext().getNumberOfParallelSubtasks();

        List<RuleJobConfig.DataSourceConfig> allDevices = config.getDataSources();
        myDevices = new ArrayList<>();
        if (allDevices != null) {
            for (int i = 0; i < allDevices.size(); i++) {
                if (i % parallelism == subtaskIndex) {
                    myDevices.add(allDevices.get(i));
                }
            }
        }

        LOG.info("TDEngineJdbcSource[subtask-{}] 分配到 {} 个设备",
                subtaskIndex, myDevices.size());

        // 初始化 JDBC 连接
        RuleJobConfig.TDEngineConfig td = config.getTdengineConfig();
        if (td != null && td.getJdbcUrl() != null) {
            connection = DriverManager.getConnection(
                    td.getJdbcUrl(), td.getUsername(), td.getPassword());
        }
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
            if (connection == null || connection.isClosed()) {
                LOG.warn("TDEngine 连接不可用，跳过本轮");
                Thread.sleep(pollIntervalMs);
                continue;
            }

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

        // 构造测点过滤
        String pointFilter = "";
        if (ds.getPointCodes() != null && !ds.getPointCodes().isEmpty()) {
            String points = ds.getPointCodes().stream()
                    .map(p -> "'" + p + "'")
                    .collect(Collectors.joining(","));
            pointFilter = " AND point_code IN (" + points + ")";
        }

        // 构造时段过滤
        String timeFilter = "";
        if (ds.getTimeRangeStart() != null && !ds.getTimeRangeStart().isEmpty()) {
            timeFilter += " AND ts >= '" + ds.getTimeRangeStart() + "'";
        }
        if (ds.getTimeRangeEnd() != null && !ds.getTimeRangeEnd().isEmpty()) {
            timeFilter += " AND ts <= '" + ds.getTimeRangeEnd() + "'";
        }

        String sql = String.format(
                "SELECT ts, device_id, point_code, value, quality FROM %s "
                        + "WHERE device_id = '%s' %s %s AND ts > '%s' ORDER BY ts ASC LIMIT 10000",
                superTable, ds.getDeviceId(), pointFilter, timeFilter,
                new Timestamp(lastTimestamp).toString()
        );

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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
                    lastTimestamp = Math.max(lastTimestamp, ts);
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
        checkpointedState.add(lastTimestamp);
    }

    @Override
    public void initializeState(FunctionInitializationContext ctx) throws Exception {
        checkpointedState = ctx.getOperatorStateStore().getListState(
                new ListStateDescriptor<>("lastTimestamp", Long.class));

        if (ctx.isRestored()) {
            for (Long ts : checkpointedState.get()) {
                lastTimestamp = ts;
            }
            LOG.info("从 Checkpoint 恢复 lastTimestamp: {}", lastTimestamp);
        }
    }
}
