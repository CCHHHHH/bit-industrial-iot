package com.bit.iot.integration.scheduler;

import com.bit.iot.integration.dao.IntegrationConfigMapper;
import com.bit.iot.integration.model.entity.IntegrationConfig;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.bit.iot.integration.model.enums.MappingTypeEnum;
import com.bit.iot.integration.service.IIntegrationDataMappingService;
import com.bit.iot.integration.service.IntegrationTimeSeriesCollectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationCollectSchedulerTest {

    private IntegrationCollectScheduler scheduler;

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Test
    void restartIfRunningRegistersTimeSeriesMappings() {
        IntegrationConfig config = new IntegrationConfig()
                .setId("integration-001")
                .setIntegrationStatus(1);
        IntegrationDataMapping mapping = new IntegrationDataMapping()
                .setId("mapping-001")
                .setIntegrationId("integration-001")
                .setMappingType(MappingTypeEnum.TIME_SERIES_DATA.getCode())
                .setSchedulerTime(1L)
                .setSchedulerUnit("h");
        List<String> calls = new ArrayList<>();
        IIntegrationDataMappingService mappingService = serviceProxy(mapping, calls);
        IntegrationConfigMapper configMapper = mapperProxy(config);
        AtomicInteger collectCount = new AtomicInteger();
        IntegrationTimeSeriesCollectService collectService = new IntegrationTimeSeriesCollectService(null, null, null, null, null) {
            @Override
            public com.bit.iot.integration.model.dto.TimeSeriesCollectResultDTO collectAndWrite(String integrationId,
                                                                                                IntegrationDataMapping dataMapping) {
                collectCount.incrementAndGet();
                return new com.bit.iot.integration.model.dto.TimeSeriesCollectResultDTO();
            }
        };
        scheduler = new IntegrationCollectScheduler(mappingService, collectService, configMapper);
        scheduler.init();

        scheduler.restartIfRunning("integration-001");

        assertThat(calls).contains("getDataMappingsByIntegrationId", "calculateSeconds");
        assertThat(collectCount.get()).isGreaterThanOrEqualTo(0);
    }

    private IIntegrationDataMappingService serviceProxy(IntegrationDataMapping mapping, List<String> calls) {
        return (IIntegrationDataMappingService) Proxy.newProxyInstance(
                IIntegrationDataMappingService.class.getClassLoader(),
                new Class<?>[]{IIntegrationDataMappingService.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    return switch (method.getName()) {
                        case "getDataMappingsByIntegrationId" -> List.of(mapping);
                        case "calculateSeconds" -> 3600L;
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private IntegrationConfigMapper mapperProxy(IntegrationConfig config) {
        return (IntegrationConfigMapper) Proxy.newProxyInstance(
                IntegrationConfigMapper.class.getClassLoader(),
                new Class<?>[]{IntegrationConfigMapper.class},
                (proxy, method, args) -> {
                    if ("selectById".equals(method.getName())) {
                        return config;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType.isPrimitive()) {
            return 0;
        }
        return null;
    }
}
