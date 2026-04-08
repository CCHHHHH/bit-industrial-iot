package com.bit.iot.integration.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.integration.model.dto.IntegrationConfigDetailDTO;
import com.bit.iot.integration.model.dto.IntegrationConfigListItemDTO;
import com.bit.iot.integration.model.dto.PluginConfigItemDTO;
import com.bit.iot.integration.model.entity.IntegrationConfig;
import com.bit.iot.integration.model.entity.IntegrationConfigParam;
import com.bit.iot.integration.model.request.IntegrationConfigRequest;
import com.bit.iot.integration.model.request.IntegrationPluginConfigRequest;
import com.bit.iot.integration.model.vo.IntegrationConfigParamVO;
import com.bit.iot.integration.model.vo.IntegrationPluginConfigVO;
import com.bit.iot.integration.model.vo.IntegrationConfigVO;
import com.bit.iot.integration.service.IIntegrationConfigParamService;
import com.bit.iot.integration.service.IIntegrationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 集成实例配置表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@RestController
@RequestMapping("/integration-config")
@Tag(name = "集成配置管理接口", description = "集成配置相关操作接口")
public class IntegrationConfigController extends BaseController {

    @Autowired
    private IIntegrationConfigService integrationConfigService;

    @Autowired
    private IIntegrationConfigParamService configParamService;

    @GetMapping("/list")
    @Operation(summary = "分页查询集成配置列表")
    public Result<List<IntegrationConfigListItemDTO>> getIntegrationConfigList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String integrationName,
            String pluginId) {
        Page<IntegrationConfig> page = new Page<>(current, size);
        Page<IntegrationConfigListItemDTO> result = integrationConfigService.getIntegrationConfigList(page, integrationName, pluginId);
        return success(result.getRecords());
    }

    @PostMapping
    @Operation(summary = "新增集成配置")
    public Result<String> addIntegrationConfig(@RequestBody IntegrationConfigRequest integrationConfig) {
        IntegrationConfig entity = new IntegrationConfig();
        BeanUtils.copyProperties(integrationConfig, entity);
        try {
            boolean success = integrationConfigService.addIntegrationConfig(entity);
            return success ? success(entity.getId()) : error("新增失败");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "编辑集成配置")
    public Result<Void> editIntegrationConfig(@RequestBody IntegrationConfigRequest integrationConfig) {
        IntegrationConfig entity = new IntegrationConfig();
        BeanUtils.copyProperties(integrationConfig, entity);
        boolean success = integrationConfigService.editIntegrationConfig(entity);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除集成配置")
    public Result<Void> deleteIntegrationConfig(@PathVariable String id) {
        boolean success = integrationConfigService.deleteIntegrationConfig(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @GetMapping("/by-plugin/{pluginId}")
    @Operation(summary = "根据插件 ID 查询集成配置列表")
    public Result<List<IntegrationConfigVO>> getIntegrationConfigsByPluginId(@PathVariable String pluginId) {
        List<IntegrationConfig> integrationConfigs = integrationConfigService.getIntegrationConfigsByPluginId(pluginId);
        return success(integrationConfigs.stream().map(this::toVO).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询单个集成配置及参数")
    public Result<IntegrationConfigDetailDTO> getIntegrationConfigDetail(@PathVariable String id) {
        // 查询集成配置
        IntegrationConfig config = integrationConfigService.getIntegrationConfigById(id);
        if (config == null) {
            return error("未找到该集成配置");
        }
        
        // 查询配置参数
        List<IntegrationConfigParam> configParams = configParamService.getConfigParamsByIntegrationId(id);

        // 组装返回结果
        IntegrationConfigDetailDTO detailDTO = new IntegrationConfigDetailDTO();
        detailDTO.setConfig(toVO(config));
        detailDTO.setConfigParams(configParams.stream().map(this::toVO).toList());
        
        return success(detailDTO);
    }

    @PutMapping("/{id}/start")
    @Operation(summary = "启动集成配置")
    public Result<Void> startIntegration(@PathVariable String id) {
        try {
            boolean success = integrationConfigService.startIntegration(id);
            return success ? success("启动成功") : error("启动失败");
        } catch (Exception e) {
            return error("启动失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/pause")
    @Operation(summary = "暂停集成配置")
    public Result<Void> pauseIntegration(@PathVariable String id) {
        try {
            boolean success = integrationConfigService.pauseIntegration(id);
            return success ? success("暂停成功") : error("暂停失败");
        } catch (Exception e) {
            return error("暂停失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}/plugin-config")
    @Operation(summary = "读取集成实例插件配置")
    public Result<IntegrationPluginConfigVO> getPluginConfig(@PathVariable String id) {
        IntegrationConfig config = integrationConfigService.getIntegrationConfigById(id);
        if (config == null) {
            return error("未找到该集成配置");
        }
        IntegrationPluginConfigVO vo = new IntegrationPluginConfigVO();
        vo.setIntegrationId(id);
        vo.setPluginId(config.getPluginId());
        vo.setConfigItems(configParamService.getConfigParamsByIntegrationId(id).stream().map(item -> {
            PluginConfigItemDTO dto = new PluginConfigItemDTO();
            dto.setKey(item.getParamKey());
            dto.setValue(item.getParamValue());
            dto.setDescription(item.getParamDesc());
            return dto;
        }).toList());
        return success(vo);
    }

    @PutMapping("/{id}/plugin-config")
    @Operation(summary = "覆盖保存集成实例插件配置")
    public Result<Void> savePluginConfig(
            @PathVariable String id,
            @RequestBody IntegrationPluginConfigRequest request) {
        IntegrationConfig config = integrationConfigService.getIntegrationConfigById(id);
        if (config == null) {
            return error("未找到该集成配置");
        }
        List<IntegrationConfigParam> paramList = (request == null ? java.util.List.<PluginConfigItemDTO>of() : request.getConfigItems())
                .stream()
                .map(item -> new IntegrationConfigParam()
                        .setId(UUID.randomUUID().toString())
                        .setIntegrationId(id)
                        .setParamKey(item.getKey())
                        .setParamValue(item.getValue())
                        .setParamDesc(item.getDescription()))
                .toList();
        boolean success = configParamService.saveConfigParamItems(id, paramList);
        return success ? success("保存成功") : error("保存失败");
    }

    private IntegrationConfigVO toVO(IntegrationConfig config) {
        IntegrationConfigVO vo = new IntegrationConfigVO();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }

    private IntegrationConfigParamVO toVO(IntegrationConfigParam param) {
        IntegrationConfigParamVO vo = new IntegrationConfigParamVO();
        BeanUtils.copyProperties(param, vo);
        return vo;
    }
}
