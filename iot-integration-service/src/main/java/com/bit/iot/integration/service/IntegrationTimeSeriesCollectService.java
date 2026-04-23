package com.bit.iot.integration.service;

import com.bit.iot.integration.model.dto.TimeSeriesCollectResultDTO;
import com.bit.iot.integration.model.dto.TimeSeriesPointDTO;
import com.bit.iot.integration.model.entity.IntegrationConfig;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.bit.iot.integration.model.enums.MappingTypeEnum;
import com.bit.iot.integration.dao.IntegrationConfigMapper;
import com.bit.iot.integration.plugin.PluginManager;
import com.bit.iot.integration.tdengine.TDEnginePointWriter;
import com.bit.iot.integration.tdengine.TimeSeriesPointNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class IntegrationTimeSeriesCollectService {

    private final IntegrationConfigMapper integrationConfigMapper;

    private final IIntegrationDataMappingService dataMappingService;

    private final PluginManager pluginManager;

    private final TimeSeriesPointNormalizer normalizer;

    private final TDEnginePointWriter pointWriter;

    public IntegrationTimeSeriesCollectService(IntegrationConfigMapper integrationConfigMapper,
                                               IIntegrationDataMappingService dataMappingService,
                                               PluginManager pluginManager,
                                               TimeSeriesPointNormalizer normalizer,
                                               TDEnginePointWriter pointWriter) {
        this.integrationConfigMapper = integrationConfigMapper;
        this.dataMappingService = dataMappingService;
        this.pluginManager = pluginManager;
        this.normalizer = normalizer;
        this.pointWriter = pointWriter;
    }

    public TimeSeriesCollectResultDTO collectAndWrite(String integrationId) {
        IntegrationConfig config = requireRunningConfig(integrationId);
        List<IntegrationDataMapping> mappings = dataMappingService.getDataMappingsByIntegrationId(integrationId)
                .stream()
                .filter(this::isTimeSeriesMapping)
                .toList();
        if (mappings.isEmpty()) {
            throw new IllegalStateException("未配置时序数据映射");
        }

        TimeSeriesCollectResultDTO summary = new TimeSeriesCollectResultDTO();
        summary.setIntegrationId(integrationId);
        for (IntegrationDataMapping mapping : mappings) {
            TimeSeriesCollectResultDTO item = collectAndWrite(config, mapping);
            summary.add(item);
        }
        summary.setMessage("采集完成");
        return summary;
    }

    public TimeSeriesCollectResultDTO collectAndWrite(String integrationId, IntegrationDataMapping mapping) {
        IntegrationConfig config = requireRunningConfig(integrationId);
        return collectAndWrite(config, mapping);
    }

    private TimeSeriesCollectResultDTO collectAndWrite(IntegrationConfig config, IntegrationDataMapping mapping) {
        if (!isTimeSeriesMapping(mapping)) {
            throw new IllegalArgumentException("非时序数据映射不能写入 TDengine");
        }
        String integrationId = config.getId();
        TimeSeriesCollectResultDTO result = new TimeSeriesCollectResultDTO();
        result.setIntegrationId(integrationId);
        result.setMappingCount(1);

        try {
            Object pluginResult = pluginManager.invokePlugin(
                    config.getPluginId(),
                    MappingTypeEnum.TIME_SERIES_DATA.getPluginMethodName(),
                    mapping.getSourceData()
            );
            List<TimeSeriesPointDTO> points = normalizer.normalize(pluginResult, Instant.now());
            int written = pointWriter.write(points);
            result.setPointCount(points.size());
            result.setWrittenCount(written);
            result.setMessage("采集并写入成功");
            log.info("集成时序采集写入完成: integrationId={}, mappingId={}, points={}, written={}",
                    integrationId, mapping.getId(), points.size(), written);
            return result;
        } catch (Exception e) {
            log.error("集成时序采集写入失败: integrationId={}, mappingId={}",
                    integrationId, mapping == null ? null : mapping.getId(), e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private IntegrationConfig requireRunningConfig(String integrationId) {
        IntegrationConfig config = integrationConfigMapper.selectById(integrationId);
        if (config == null) {
            throw new IllegalArgumentException("集成实例不存在：" + integrationId);
        }
        if (config.getIntegrationStatus() == null || config.getIntegrationStatus() != 1) {
            throw new IllegalStateException("集成实例未启动，无法采集时序数据");
        }
        if (config.getPluginId() == null || config.getPluginId().isBlank()) {
            throw new IllegalStateException("集成实例未绑定插件");
        }
        return config;
    }

    private boolean isTimeSeriesMapping(IntegrationDataMapping mapping) {
        return mapping != null
                && MappingTypeEnum.TIME_SERIES_DATA.getCode().equals(mapping.getMappingType());
    }
}
