package com.bit.iot.simulator.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bit.iot.simulator.dao.SimulatorDeviceMapper;
import com.bit.iot.simulator.dao.SimulatorPointMapper;
import com.bit.iot.simulator.dao.SimulatorSendLogMapper;
import com.bit.iot.simulator.dao.SimulatorTaskMapper;
import com.bit.iot.simulator.dao.SimulatorTimeseriesDataMapper;
import com.bit.iot.simulator.model.entity.*;
import com.bit.iot.simulator.model.enums.ProtocolTypeEnum;
import com.bit.iot.simulator.model.enums.TaskStatusEnum;
import com.bit.iot.simulator.service.protocol.ProtocolPayload;
import com.bit.iot.simulator.service.protocol.ProtocolSendResult;
import com.bit.iot.simulator.service.protocol.ProtocolSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
/**
 * 模拟任务运行时调度管理器。
 */
public class SimulatorTaskRuntimeManager {

    /**
     * 模拟任务 Mapper。
     */
    private final SimulatorTaskMapper taskMapper;
    /**
     * 模拟设备 Mapper。
     */
    private final SimulatorDeviceMapper deviceMapper;
    /**
     * 模拟测点 Mapper。
     */
    private final SimulatorPointMapper pointMapper;
    /**
     * 模拟时序数据 Mapper。
     */
    private final SimulatorTimeseriesDataMapper timeseriesDataMapper;
    /**
     * 模拟发送日志 Mapper。
     */
    private final SimulatorSendLogMapper sendLogMapper;
    /**
     * 任务调度器。
     */
    private final ThreadPoolTaskScheduler scheduler;
    /**
     * 协议发送器集合。
     */
    private final List<ProtocolSender> protocolSenders;
    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 随机值生成器。
     */
    private final Random random = new Random();
    /**
     * 已调度任务句柄缓存。
     */
    private final Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    /**
     * 构造模拟任务运行时调度管理器。
     *
     * @param taskMapper 模拟任务 Mapper
     * @param deviceMapper 模拟设备 Mapper
     * @param pointMapper 模拟测点 Mapper
     * @param timeseriesDataMapper 模拟时序数据 Mapper
     * @param sendLogMapper 模拟发送日志 Mapper
     * @param scheduler 任务调度器
     * @param protocolSenders 协议发送器集合
     */
    public SimulatorTaskRuntimeManager(SimulatorTaskMapper taskMapper,
                                       SimulatorDeviceMapper deviceMapper,
                                       SimulatorPointMapper pointMapper,
                                       SimulatorTimeseriesDataMapper timeseriesDataMapper,
                                       SimulatorSendLogMapper sendLogMapper,
                                       ThreadPoolTaskScheduler scheduler,
                                       List<ProtocolSender> protocolSenders) {
        this.taskMapper = taskMapper;
        this.deviceMapper = deviceMapper;
        this.pointMapper = pointMapper;
        this.timeseriesDataMapper = timeseriesDataMapper;
        this.sendLogMapper = sendLogMapper;
        this.scheduler = scheduler;
        this.protocolSenders = protocolSenders;
    }

    /**
     * 应用启动后恢复运行中的任务调度。
     */
    @PostConstruct
    public void restoreRunningTasks() {
        List<SimulatorTask> tasks = taskMapper.selectList(new QueryWrapper<SimulatorTask>()
                .eq("task_status", TaskStatusEnum.RUNNING.name()));
        // 仅恢复数据库中标记为运行中的任务，避免重复拉起已停止任务。
        for (SimulatorTask task : tasks) {
            scheduleTask(task);
        }
    }

    /**
     * 为任务创建或重建定时调度。
     *
     * @param task 任务实体
     */
    public void scheduleTask(SimulatorTask task) {
        // 重建调度前先取消旧任务，避免同一任务被重复调度。
        cancelTask(task.getId());
        // 为异常配置提供兜底频率，避免过高调度频率压垮线程池。
        long frequencyMs = task.getFrequencyMs() == null || task.getFrequencyMs() < 100
                ? 1000L : task.getFrequencyMs();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> executeTask(task.getId()), frequencyMs);
        futureMap.put(task.getId(), future);
    }

    /**
     * 取消任务调度。
     *
     * @param taskId 任务主键
     */
    public void cancelTask(String taskId) {
        ScheduledFuture<?> future = futureMap.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 执行一次任务调度逻辑。
     *
     * @param taskId 任务主键
     */
    public void executeTask(String taskId) {
        SimulatorTask task = taskMapper.selectById(taskId);
        // 任务不存在或已非运行态时，立即回收调度句柄，避免无效轮询。
        if (task == null || !TaskStatusEnum.RUNNING.name().equals(task.getTaskStatus())) {
            cancelTask(taskId);
            return;
        }
        SimulatorDevice device = deviceMapper.selectById(task.getDeviceId());
        if (device == null) {
            markFailure(task, "设备不存在");
            return;
        }
        List<SimulatorPoint> points = pointMapper.selectList(new QueryWrapper<SimulatorPoint>()
                .eq("device_id", device.getId())
                .orderByAsc("point_code"));
        if (points.isEmpty()) {
            markFailure(task, "设备未配置测点");
            return;
        }

        Date now = new Date();
        // 使用有序映射保持测点顺序稳定，便于日志与报文排查。
        Map<String, Object> values = new LinkedHashMap<>();
        for (SimulatorPoint point : points) {
            double value = nextRandomValue(point);
            values.put(point.getPointCode(), value);

            // 每次任务执行都落一份时序明细，便于后续历史查询与问题追踪。
            SimulatorTimeseriesData data = new SimulatorTimeseriesData();
            data.setTaskId(task.getId());
            data.setDeviceId(device.getId());
            data.setPointCode(point.getPointCode());
            data.setPointName(point.getPointName());
            data.setPointValue(value);
            data.setUnit(point.getUnit());
            data.setQuality(point.getQuality() == null ? 0 : point.getQuality());
            data.setProtocolType(task.getProtocolType());
            data.setGeneratedTime(now);
            timeseriesDataMapper.insert(data);
        }

        // 构造协议无关的标准负载，再交由具体协议发送器适配输出。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("taskName", task.getTaskName());
        payload.put("protocolType", task.getProtocolType());
        payload.put("timestamp", now.getTime());
        payload.put("deviceId", device.getId());
        payload.put("deviceCode", device.getDeviceCode());
        payload.put("deviceName", device.getDeviceName());
        payload.put("values", values);

        ProtocolPayload protocolPayload = new ProtocolPayload();
        protocolPayload.setTask(task);
        protocolPayload.setDevice(device);
        protocolPayload.setPoints(points);
        protocolPayload.setPayload(payload);
        protocolPayload.setGeneratedTime(now);

        ProtocolSendResult sendResult;
        try {
            // 协议发送器按任务配置动态解析，避免运行时出现协议分支散落。
            ProtocolSender protocolSender = resolveSender(task.getProtocolType());
            sendResult = protocolSender.send(protocolPayload);
        } catch (Exception e) {
            log.error("设备模拟发送失败: taskId={}", task.getId(), e);
            sendResult = ProtocolSendResult.failure(e.getMessage());
        }

        // 无论发送成功还是失败，都记录发送日志，保证审计链完整。
        SimulatorSendLog sendLog = new SimulatorSendLog();
        sendLog.setTaskId(task.getId());
        sendLog.setDeviceId(device.getId());
        sendLog.setProtocolType(task.getProtocolType());
        try {
            // 优先落标准 JSON，序列化失败时退化为字符串，避免日志写入再次失败。
            sendLog.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            sendLog.setPayloadJson(String.valueOf(payload));
        }
        sendLog.setSendStatus(sendResult.isSuccess() ? "SUCCESS" : "FAILED");
        sendLog.setErrorMessage(sendResult.isSuccess() ? null : sendResult.getMessage());
        sendLog.setSentTime(now);
        sendLogMapper.insert(sendLog);

        // 任务表只保留最近一次执行状态，供列表页快速展示。
        task.setLastSentTime(now);
        task.setLastError(sendResult.isSuccess() ? null : sendResult.getMessage());
        task.setUpdateTime(now);
        taskMapper.updateById(task);
    }

    /**
     * 手动触发一次任务执行。
     *
     * @param taskId 任务主键
     */
    public void triggerOnce(String taskId) {
        executeTask(taskId);
    }

    /**
     * 按协议类型解析发送器。
     *
     * @param protocolType 协议类型
     * @return 协议发送器
     */
    private ProtocolSender resolveSender(String protocolType) {
        ProtocolTypeEnum target = ProtocolTypeEnum.safeValueOf(protocolType);
        return protocolSenders.stream()
                // 通过协议枚举匹配具体实现，避免硬编码实现类依赖。
                .filter(item -> item.protocolType() == target)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的协议类型: " + protocolType));
    }

    /**
     * 标记任务失败信息。
     *
     * @param task 任务实体
     * @param errorMessage 错误信息
     */
    private void markFailure(SimulatorTask task, String errorMessage) {
        task.setLastError(errorMessage);
        task.setUpdateTime(new Date());
        taskMapper.updateById(task);
    }

    /**
     * 生成测点随机值并按精度要求进行四舍五入。
     *
     * @param point 测点实体
     * @return 随机值
     */
    private double nextRandomValue(SimulatorPoint point) {
        double min = point.getMinValue() == null ? 0D : point.getMinValue();
        double max = point.getMaxValue() == null ? 100D : point.getMaxValue();
        if (max < min) {
            // 自动纠正上下限配置反转，避免生成负区间数据。
            double temp = min;
            min = max;
            max = temp;
        }
        double raw = min + (max - min) * random.nextDouble();
        // 按测点精度统一收口，避免不同协议输出出现精度漂移。
        int scale = point.getPrecisionScale() == null ? 2 : Math.max(point.getPrecisionScale(), 0);
        return BigDecimal.valueOf(raw).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
