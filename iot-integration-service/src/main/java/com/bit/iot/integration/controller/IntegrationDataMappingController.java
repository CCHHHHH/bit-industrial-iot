package com.bit.iot.integration.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.bit.iot.integration.service.IIntegrationDataMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 集成实例数据映射表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@RestController
@RequestMapping("/integration-data-mapping")
@Tag(name = "数据映射管理接口", description = "数据映射相关操作接口")
public class IntegrationDataMappingController extends BaseController {

    @Autowired
    private IIntegrationDataMappingService dataMappingService;

    @GetMapping("/list")
    @Operation(summary = "分页查询数据映射列表")
    public Result<List<IntegrationDataMapping>> getDataMappingList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String integrationId) {
        Page<IntegrationDataMapping> page = new Page<>(current, size);
        Page<IntegrationDataMapping> result = dataMappingService.getDataMappingList(page, integrationId);
        return success(result);
    }

    @PostMapping
    @Operation(summary = "新增数据映射")
    public Result<Void> addDataMapping(@RequestBody IntegrationDataMapping dataMapping) {
        boolean success = dataMappingService.addDataMapping(dataMapping);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑数据映射")
    public Result<Void> editDataMapping(@RequestBody IntegrationDataMapping dataMapping) {
        boolean success = dataMappingService.editDataMapping(dataMapping);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据映射")
    public Result<Void> deleteDataMapping(@PathVariable String id) {
        boolean success = dataMappingService.deleteDataMapping(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @GetMapping("/by-integration/{integrationId}")
    @Operation(summary = "根据集成配置 ID 查询数据映射列表")
    public Result<List<IntegrationDataMapping>> getDataMappingsByIntegrationId(@PathVariable String integrationId) {
        List<IntegrationDataMapping> dataMappings = dataMappingService.getDataMappingsByIntegrationId(integrationId);
        return success(dataMappings);
    }
}
