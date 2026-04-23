package com.bit.iot.integration.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.bit.iot.integration.model.entity.IntegrationConfig;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.bit.iot.integration.model.dto.TimeSeriesCollectResultDTO;
import com.bit.iot.integration.model.enums.MappingTypeEnum;
import com.bit.iot.integration.plugin.PluginManager;
import com.bit.iot.integration.service.IIntegrationConfigService;
import com.bit.iot.integration.service.IIntegrationDataMappingService;
import com.bit.iot.integration.service.IntegrationTimeSeriesCollectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 插件数据获取接口
 * 根据 MappingTypeEnum 调用对应插件方法获取设备属性、设备状态、时序数据
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/integration-data")
@Tag(name = "插件数据接口", description = "通过插件获取设备属性、设备状态、时序数据")
public class IntegrationDataController extends BaseController {

    @Autowired
    private IIntegrationConfigService configService;

    @Autowired
    private IIntegrationDataMappingService dataMappingService;

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private IntegrationTimeSeriesCollectService timeSeriesCollectService;

    /**
     * 获取设备属性
     * 根据集成实例查找 mapping_type=device_property 的数据映射，
     * 调用插件的 handleDeviceProperty 方法并返回结果。
     */
    @GetMapping("/{integrationId}/device-property")
    @Operation(summary = "获取设备属性", description = "调用插件 handleDeviceProperty 方法获取设备属性数据")
    public Result<Object> getDeviceProperty(
            @Parameter(description = "集成实例 ID", required = true)
            @PathVariable String integrationId) {
        return invokePluginByMappingType(integrationId, MappingTypeEnum.DEVICE_PROPERTY);
    }

    /**
     * 获取设备状态
     * 根据集成实例查找 mapping_type=device_status 的数据映射，
     * 调用插件的 handleDeviceStatus 方法并返回结果。
     */
    @GetMapping("/{integrationId}/device-status")
    @Operation(summary = "获取设备状态", description = "调用插件 handleDeviceStatus 方法获取设备状态数据")
    public Result<Object> getDeviceStatus(
            @Parameter(description = "集成实例 ID", required = true)
            @PathVariable String integrationId) {
        return invokePluginByMappingType(integrationId, MappingTypeEnum.DEVICE_STATUS);
    }

    /**
     * 获取时序数据
     * 根据集成实例查找 mapping_type=time_series_data 的数据映射，
     * 调用插件的 handleTimeSeriesData 方法并返回结果。
     */
    @GetMapping("/{integrationId}/time-series")
    @Operation(summary = "获取时序数据", description = "调用插件 handleTimeSeriesData 方法获取时序数据")
    public Result<Object> getTimeSeriesData(
            @Parameter(description = "集成实例 ID", required = true)
            @PathVariable String integrationId) {
        return invokePluginByMappingType(integrationId, MappingTypeEnum.TIME_SERIES_DATA);
    }

    @PostMapping("/{integrationId}/time-series/collect-once")
    @Operation(summary = "手动采集并写入时序数据", description = "调用插件 handleTimeSeriesData 方法，并将返回点位写入 TDengine")
    public Result<TimeSeriesCollectResultDTO> collectTimeSeriesOnce(
            @Parameter(description = "集成实例 ID", required = true)
            @PathVariable String integrationId) {
        try {
            return success(timeSeriesCollectService.collectAndWrite(integrationId));
        } catch (Exception e) {
            return error("采集写入失败：" + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 公共调用逻辑
    // -----------------------------------------------------------------------

    /**
     * 根据集成实例 ID 和映射类型调用对应插件方法
     *
     * @param integrationId 集成实例 ID
     * @param mappingType   映射类型枚举
     * @return 插件执行结果
     */
    private Result<Object> invokePluginByMappingType(String integrationId, MappingTypeEnum mappingType) {
        // 1. 查询集成实例
        IntegrationConfig config = configService.getById(integrationId);
        if (config == null) {
            return error("集成实例不存在：" + integrationId);
        }

        // 2. 校验集成实例状态
        if (config.getIntegrationStatus() == null || config.getIntegrationStatus() != 1) {
            return error("集成实例未启动，无法获取数据");
        }

        // 3. 查询对应映射类型的数据映射配置
        IntegrationDataMapping dataMapping = dataMappingService.getDataMappingByType(
                integrationId, mappingType.getCode());
        if (dataMapping == null) {
            return error("未找到 [" + mappingType.getDescription() + "] 类型的数据映射配置");
        }

        // 4. 调用插件对应方法
        try {
            Object result = pluginManager.invokePlugin(
                    config.getPluginId(),
                    mappingType.getPluginMethodName(),
                    dataMapping.getSourceData());
            return success(result);
        } catch (Exception e) {
            return error("调用插件失败：" + e.getMessage());
        }
    }
}
