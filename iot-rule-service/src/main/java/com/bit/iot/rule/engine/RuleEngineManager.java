package com.bit.iot.rule.engine;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.rule.model.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * 规则引擎管理器
 * <p>
 * 负责规则的启动、停止和定时调度执行。
 * 采用线程池模拟 Flink 本地执行，预留 Flink Job 提交扩展点。
 * </p>
 *
 * <p>执行流程：</p>
 * <ol>
 *   <li>根据规则配置加载数据源（设备 + 测点 + 时段）</li>
 *   <li>从 TDEngine 查询时序数据（通过 TDEngineDataReader）</li>
 *   <li>调用 AlgorithmLoader 执行算法（JAR / Python）</li>
 *   <li>将执行结果写入 rule_execution_log</li>
 * </ol>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Slf4j
@Component
public class RuleEngineManager {

    @Autowired
    private AlgorithmLoader algorithmLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 正在运行的规则调度任务：ruleId -> ScheduledFuture */
    private final Map<String, ScheduledFuture<?>> runningJobs = new ConcurrentHashMap<>();

    /** 共享调度线程池 */
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    // -----------------------------------------------------------------------
    // 规则生命周期
    // -----------------------------------------------------------------------

    /**
     * 启动规则
     *
     * @param ruleConfig  规则配置
     * @param algorithm   算法元数据
     * @param dataSources 数据源配置列表
     * @param params      参数列表
     * @param logConsumer 执行日志回调（由 Service 层处理持久化）
     */
    public void startRule(RuleConfig ruleConfig,
                          RuleAlgorithm algorithm,
                          List<RuleDataSource> dataSources,
                          List<RuleParam> params,
                          RuleExecutionLogConsumer logConsumer) {

        String ruleId = ruleConfig.getId();
        if (runningJobs.containsKey(ruleId)) {
            log.warn("规则已在运行中，跳过启动：{}", ruleId);
            return;
        }

        // 参数 Map
        Map<String, String> paramMap = new HashMap<>();
        if (params != null) {
            params.forEach(p -> paramMap.put(p.getParamKey(), p.getParamValue()));
        }

        // 窗口大小（毫秒）
        long windowMs = resolveWindowMs(ruleConfig);

        Runnable task = () -> executeRule(ruleId, ruleConfig, algorithm, dataSources, paramMap, logConsumer);

        ScheduledFuture<?> future;
        String triggerType = ruleConfig.getTriggerType();

        if ("periodic".equalsIgnoreCase(triggerType)) {
            // 定时触发：以窗口大小为周期
            long periodMs = windowMs > 0 ? windowMs : 60_000L;
            future = scheduler.scheduleAtFixedRate(task, 0, periodMs, TimeUnit.MILLISECONDS);
            log.info("规则以定时模式启动，周期 {}ms：{}", periodMs, ruleConfig.getRuleName());
        } else {
            // 实时模式：立即执行一次，后续由外部触发（此处用较短轮询周期模拟）
            future = scheduler.scheduleAtFixedRate(task, 0, windowMs > 0 ? windowMs : 5_000L, TimeUnit.MILLISECONDS);
            log.info("规则以实时模式启动：{}", ruleConfig.getRuleName());
        }

        runningJobs.put(ruleId, future);
    }

    /**
     * 停止规则
     *
     * @param ruleId 规则 ID
     */
    public void stopRule(String ruleId) {
        ScheduledFuture<?> future = runningJobs.remove(ruleId);
        if (future != null) {
            future.cancel(false);
            log.info("规则已停止：{}", ruleId);
        } else {
            log.warn("规则未在运行中，无法停止：{}", ruleId);
        }
    }

    /**
     * 判断规则是否正在运行
     */
    public boolean isRunning(String ruleId) {
        return runningJobs.containsKey(ruleId);
    }

    /**
     * 手动触发执行一次（不受调度周期限制）
     */
    public void triggerOnce(RuleConfig ruleConfig,
                            RuleAlgorithm algorithm,
                            List<RuleDataSource> dataSources,
                            List<RuleParam> params,
                            RuleExecutionLogConsumer logConsumer) {
        Map<String, String> paramMap = new HashMap<>();
        if (params != null) {
            params.forEach(p -> paramMap.put(p.getParamKey(), p.getParamValue()));
        }
        scheduler.submit(() ->
                executeRule(ruleConfig.getId(), ruleConfig, algorithm, dataSources, paramMap, logConsumer));
    }

    // -----------------------------------------------------------------------
    // 核心执行逻辑
    // -----------------------------------------------------------------------

    private void executeRule(String ruleId,
                             RuleConfig ruleConfig,
                             RuleAlgorithm algorithm,
                             List<RuleDataSource> dataSources,
                             Map<String, String> paramMap,
                             RuleExecutionLogConsumer logConsumer) {

        log.debug("开始执行规则：{}", ruleConfig.getRuleName());
        long startMs = System.currentTimeMillis();
        RuleExecutionLog executionLog = new RuleExecutionLog();
        executionLog.setRuleId(ruleId);
        executionLog.setStartTime(new Date(startMs));
        executionLog.setExecStatus(0); // 执行中

        // 1. 通知 Service 创建执行日志
        logConsumer.onCreate(executionLog);

        try {
            // 2. 读取时序数据
            // TODO: 接入真实 TDEngine 数据源，当前返回空列表以供框架运行
            List<DataPoint> dataPoints = readDataPoints(dataSources, ruleConfig);

            // 3. 执行算法
            AlgorithmResult result = algorithmLoader.execute(algorithm, dataPoints, paramMap);

            long durationMs = System.currentTimeMillis() - startMs;
            executionLog.setEndTime(new Date());
            executionLog.setDurationMs(durationMs);

            if (result.isSuccess()) {
                executionLog.setExecStatus(1); // 成功
                executionLog.setResultData(objectMapper.writeValueAsString(result.getData()));
                log.info("规则执行成功：{}，耗时 {}ms", ruleConfig.getRuleName(), durationMs);
            } else {
                executionLog.setExecStatus(2); // 失败
                executionLog.setErrorMsg(result.getErrorMsg());
                log.warn("规则执行失败：{}，原因：{}", ruleConfig.getRuleName(), result.getErrorMsg());
            }

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            executionLog.setEndTime(new Date());
            executionLog.setDurationMs(durationMs);
            executionLog.setExecStatus(2);
            executionLog.setErrorMsg(e.getMessage());
            log.error("规则执行异常：{}", ruleConfig.getRuleName(), e);
        }

        // 4. 更新执行日志
        logConsumer.onComplete(executionLog);
    }

    /**
     * 从 TDEngine 读取时序数据点
     * <p>
     * 预留接入点：实际项目中需注入 TDEngine JDBC 连接，
     * 根据 dataSources 中的 deviceId、pointCodes、timeRangeStart/End 拼接 SQL 查询。
     * </p>
     */
    private List<DataPoint> readDataPoints(List<RuleDataSource> dataSources, RuleConfig ruleConfig) {
        // TODO: 接入 TDEngine 实现
        // SELECT ts, device_id, point_code, value FROM bit_iot.device_data
        // WHERE device_id IN (...) AND point_code IN (...) AND ts BETWEEN ... AND ...
        log.debug("TDEngine 数据读取预留，数据源数量：{}", dataSources == null ? 0 : dataSources.size());
        return Collections.emptyList();
    }

    private long resolveWindowMs(RuleConfig ruleConfig) {
        Long size = ruleConfig.getWindowSize();
        String unit = ruleConfig.getWindowUnit();
        if (size == null || size <= 0) return 0;
        return com.bit.iot.rule.model.enums.WindowUnitEnum.toMillis(size, unit);
    }

    // -----------------------------------------------------------------------
    // 关闭
    // -----------------------------------------------------------------------

    public void shutdown() {
        runningJobs.forEach((id, f) -> f.cancel(false));
        runningJobs.clear();
        scheduler.shutdown();
        log.info("规则引擎已关闭");
    }

    // -----------------------------------------------------------------------
    // 回调接口（避免循环依赖，Service 实现此接口）
    // -----------------------------------------------------------------------

    @FunctionalInterface
    public interface RuleExecutionLogConsumer {
        void onCreate(RuleExecutionLog log);

        default void onComplete(RuleExecutionLog log) {}
    }
}
