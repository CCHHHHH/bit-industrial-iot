package com.bit.iot.integration.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.bit.iot.integration.model.entity.IntegrationConfigParam;
import com.bit.iot.integration.service.IIntegrationConfigParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 集成实例配置参数表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-20 11:32:38
 */
@RestController
@RequestMapping("/integration-config-param")
@Tag(name = "集成实例参数管理接口", description = "集成实例配置参数相关操作接口")
public class IntegrationConfigParamController extends BaseController {

    @Autowired
    private IIntegrationConfigParamService configParamService;

    @GetMapping("/list/{integrationId}")
    @Operation(summary = "查询集成实例参数列表")
    public Result<List<IntegrationConfigParam>> getConfigParamList(
            @Parameter(description = "集成实例 ID", required = true)
            @PathVariable String integrationId) {
        List<IntegrationConfigParam> list = configParamService.getConfigParamsByIntegrationId(integrationId);
        return success(list);
    }

    @PostMapping
    @Operation(summary = "新增配置参数")
    public Result<Void> addConfigParam(@RequestBody IntegrationConfigParam configParam) {
        boolean success = configParamService.addConfigParam(configParam);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑配置参数")
    public Result<Void> editConfigParam(@RequestBody IntegrationConfigParam configParam) {
        boolean success = configParamService.editConfigParam(configParam);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置参数")
    public Result<Void> deleteConfigParam(@PathVariable String id) {
        boolean success = configParamService.deleteConfigParam(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @PostMapping("/batch/{integrationId}")
    @Operation(summary = "批量保存配置参数（先删后存）",
               description = "覆盖指定集成实例的全部参数，传入空列表则清空所有参数")
    public Result<Void> saveConfigParams(
            @Parameter(description = "集成实例 ID", required = true)
            @PathVariable String integrationId,
            @RequestBody List<IntegrationConfigParam> paramList) {
        boolean success = configParamService.saveConfigParams(integrationId, paramList);
        return success ? success("保存成功") : error("保存失败");
    }
}
