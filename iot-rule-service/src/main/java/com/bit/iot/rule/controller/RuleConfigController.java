package com.bit.iot.rule.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.rule.model.dto.RuleConfigDetailDTO;
import com.bit.iot.rule.model.dto.RuleConfigListItemDTO;
import com.bit.iot.rule.model.entity.RuleConfig;
import com.bit.iot.rule.model.entity.RuleDataSource;
import com.bit.iot.rule.model.entity.RuleParam;
import com.bit.iot.rule.flink.FlinkJobManager;
import com.bit.iot.rule.flink.FlinkJobStatus;
import com.bit.iot.rule.model.request.RuleConfigRequest;
import com.bit.iot.rule.model.request.RuleDataSourceRequest;
import com.bit.iot.rule.model.request.RuleParamRequest;
import com.bit.iot.rule.service.IRuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则配置管理接口
 *
 * @author chenhao
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/rule-config")
@Tag(name = "规则配置管理接口", description = "规则的增删改查、启停及数据源/参数配置")
public class RuleConfigController extends BaseController {

    @Autowired
    private IRuleConfigService ruleConfigService;

    @Autowired
    private FlinkJobManager flinkJobManager;

    @GetMapping("/list")
    @Operation(summary = "分页查询规则列表")
    public Result<List<RuleConfigListItemDTO>> getRuleConfigList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String ruleName,
            String algorithmId) {
        Page<RuleConfig> page = new Page<>(current, size);
        return success(ruleConfigService.getRuleConfigList(page, ruleName, algorithmId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询规则详情（含数据源和参数）")
    public Result<RuleConfigDetailDTO> getRuleConfigDetail(@PathVariable String id) {
        RuleConfigDetailDTO detail = ruleConfigService.getRuleConfigDetail(id);
        return detail != null ? success(detail) : error("规则不存在");
    }

    @PostMapping
    @Operation(summary = "新增规则")
    public Result<String> addRuleConfig(@RequestBody RuleConfigRequest ruleConfig) {
        RuleConfig entity = new RuleConfig();
        BeanUtils.copyProperties(ruleConfig, entity);
        try {
            boolean ok = ruleConfigService.addRuleConfig(entity);
            return ok ? success(entity.getId()) : error("新增失败");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "编辑规则")
    public Result<Void> editRuleConfig(@RequestBody RuleConfigRequest ruleConfig) {
        RuleConfig entity = new RuleConfig();
        BeanUtils.copyProperties(ruleConfig, entity);
        return ruleConfigService.editRuleConfig(entity) ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除规则（同时删除数据源、参数和日志）")
    public Result<Void> deleteRuleConfig(@PathVariable String id) {
        try {
            return ruleConfigService.deleteRuleConfig(id) ? success("删除成功") : error("删除失败");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 数据源和参数配置
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/data-sources")
    @Operation(summary = "保存数据源配置（设备 + 测点 + 时段，先删后存）")
    public Result<Void> saveDataSources(@PathVariable String id,
                                        @RequestBody List<RuleDataSourceRequest> dataSources) {
        List<RuleDataSource> entities = (dataSources == null ? java.util.List.<RuleDataSourceRequest>of() : dataSources).stream().map(item -> {
            RuleDataSource entity = new RuleDataSource();
            BeanUtils.copyProperties(item, entity);
            return entity;
        }).toList();
        return ruleConfigService.saveDataSources(id, entities) ? success("保存成功") : error("保存失败");
    }

    @PostMapping("/{id}/params")
    @Operation(summary = "保存规则参数（先删后存）")
    public Result<Void> saveParams(@PathVariable String id,
                                   @RequestBody List<RuleParamRequest> params) {
        List<RuleParam> entities = (params == null ? java.util.List.<RuleParamRequest>of() : params).stream().map(item -> {
            RuleParam entity = new RuleParam();
            BeanUtils.copyProperties(item, entity);
            return entity;
        }).toList();
        return ruleConfigService.saveParams(id, entities) ? success("保存成功") : error("保存失败");
    }

    // -----------------------------------------------------------------------
    // 规则生命周期
    // -----------------------------------------------------------------------

    @PutMapping("/{id}/start")
    @Operation(summary = "启动规则")
    public Result<Void> startRule(@PathVariable String id) {
        try {
            return ruleConfigService.startRule(id) ? success("启动成功") : error("启动失败");
        } catch (RuntimeException e) {
            return error("启动失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/stop")
    @Operation(summary = "停止规则")
    public Result<Void> stopRule(@PathVariable String id) {
        try {
            return ruleConfigService.stopRule(id) ? success("停止成功") : error("停止失败");
        } catch (RuntimeException e) {
            return error("停止失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "手动触发执行一次")
    public Result<Void> triggerOnce(@PathVariable String id) {
        try {
            ruleConfigService.triggerOnce(id);
            return success("已触发，请查看执行日志");
        } catch (RuntimeException e) {
            return error("触发失败：" + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Flink 状态监控
    // -----------------------------------------------------------------------

    @GetMapping("/{id}/flink-status")
    @Operation(summary = "查询规则对应的 Flink Job 实时状态")
    public Result<FlinkJobStatus> getFlinkJobStatus(@PathVariable String id) {
        FlinkJobStatus status = flinkJobManager.getJobStatus(id);
        return success(status);
    }
}
