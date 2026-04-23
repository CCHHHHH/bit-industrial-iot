package com.bit.iot.integration.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bit.iot.integration.dao.IntegrationConfigMapper;
import com.bit.iot.integration.model.entity.IntegrationConfig;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.bit.iot.integration.model.enums.MappingTypeEnum;
import com.bit.iot.integration.service.IIntegrationDataMappingService;
import com.bit.iot.integration.service.IntegrationTimeSeriesCollectService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class IntegrationCollectScheduler {

    private final IIntegrationDataMappingService dataMappingService;

    private final IntegrationTimeSeriesCollectService collectService;

    private final IntegrationConfigMapper integrationConfigMapper;

    private final Map<String, List<ScheduledFuture<?>>> tasks = new ConcurrentHashMap<>();

    private ThreadPoolTaskScheduler taskScheduler;

    public IntegrationCollectScheduler(IIntegrationDataMappingService dataMappingService,
                                       IntegrationTimeSeriesCollectService collectService,
                                       IntegrationConfigMapper integrationConfigMapper) {
        this.dataMappingService = dataMappingService;
        this.collectService = collectService;
        this.integrationConfigMapper = integrationConfigMapper;
    }

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("integration-collect-");
        taskScheduler.setPoolSize(4);
        taskScheduler.initialize();
    }

    public void startIntegration(String integrationId) {
        stopIntegration(integrationId);
        List<IntegrationDataMapping> mappings = dataMappingService.getDataMappingsByIntegrationId(integrationId)
                .stream()
                .filter(this::isTimeSeriesMapping)
                .toList();
        if (mappings.isEmpty()) {
            log.info("集成实例未配置时序映射，跳过采集调度: integrationId={}", integrationId);
            return;
        }

        List<ScheduledFuture<?>> futures = new ArrayList<>();
        for (IntegrationDataMapping mapping : mappings) {
            Long seconds = dataMappingService.calculateSeconds(mapping.getSchedulerTime(), mapping.getSchedulerUnit());
            if (seconds == null || seconds <= 0) {
                log.warn("时序映射调度周期无效，跳过: integrationId={}, mappingId={}, time={}, unit={}",
                        integrationId, mapping.getId(), mapping.getSchedulerTime(), mapping.getSchedulerUnit());
                continue;
            }
            PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(seconds));
            trigger.setInitialDelay(Duration.ZERO);
            ScheduledFuture<?> future = taskScheduler.schedule(() -> runSafely(integrationId, mapping), trigger);
            if (future != null) {
                futures.add(future);
            }
            log.info("已注册集成时序采集任务: integrationId={}, mappingId={}, period={}s",
                    integrationId, mapping.getId(), seconds);
        }

        if (!futures.isEmpty()) {
            tasks.put(integrationId, futures);
        }
    }

    public void stopIntegration(String integrationId) {
        List<ScheduledFuture<?>> futures = tasks.remove(integrationId);
        if (futures == null || futures.isEmpty()) {
            return;
        }
        for (ScheduledFuture<?> future : futures) {
            future.cancel(false);
        }
        log.info("已取消集成时序采集任务: integrationId={}, count={}", integrationId, futures.size());
    }

    public void restartIfRunning(String integrationId) {
        if (integrationId == null || integrationId.isBlank()) {
            return;
        }
        IntegrationConfig config = integrationConfigMapper.selectById(integrationId);
        if (config == null || config.getIntegrationStatus() == null || config.getIntegrationStatus() != 1) {
            stopIntegration(integrationId);
            return;
        }
        startIntegration(integrationId);
    }

    public void recoverRunningIntegrations() {
        List<IntegrationConfig> runningConfigs = integrationConfigMapper.selectList(new QueryWrapper<IntegrationConfig>()
                .eq("integration_status", 1));
        for (IntegrationConfig config : runningConfigs) {
            startIntegration(config.getId());
        }
    }

    @PreDestroy
    public void shutdown() {
        for (String integrationId : List.copyOf(tasks.keySet())) {
            stopIntegration(integrationId);
        }
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }

    private void runSafely(String integrationId, IntegrationDataMapping mapping) {
        try {
            collectService.collectAndWrite(integrationId, mapping);
        } catch (Exception e) {
            log.error("集成时序采集任务执行失败: integrationId={}, mappingId={}",
                    integrationId, mapping == null ? null : mapping.getId(), e);
        }
    }

    private boolean isTimeSeriesMapping(IntegrationDataMapping mapping) {
        return mapping != null
                && MappingTypeEnum.TIME_SERIES_DATA.getCode().equals(mapping.getMappingType());
    }
}
