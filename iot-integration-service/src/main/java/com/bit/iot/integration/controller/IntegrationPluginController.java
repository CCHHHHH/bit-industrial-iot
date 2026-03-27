package com.bit.iot.integration.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.integration.model.entity.IntegrationPlugin;
import com.bit.iot.integration.plugin.PluginManager;
import com.bit.iot.integration.plugin.PluginWrapper;
import com.bit.iot.integration.service.IIntegrationPluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 集成管理插件表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@RestController
@RequestMapping("/integration-plugin")
@Tag(name = "插件管理接口", description = "插件相关操作接口")
public class IntegrationPluginController extends BaseController {

    @Autowired
    private IIntegrationPluginService pluginService;

    @Autowired
    private PluginManager pluginManager;

    @GetMapping("/list")
    @Operation(summary = "分页查询插件列表")
    public Result<List<IntegrationPlugin>> getPluginList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String pluginName) {
        Page<IntegrationPlugin> page = new Page<>(current, size);
        Page<IntegrationPlugin> result = pluginService.getPluginList(page, pluginName);
        return success(result);
    }

    @PostMapping
    @Operation(summary = "新增插件")
    public Result<Void> addPlugin(@RequestBody IntegrationPlugin plugin) {
        try {
            boolean success = pluginService.addPlugin(plugin);
            return success ? success("新增成功") : error("新增失败");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "编辑插件")
    public Result<Void> editPlugin(@RequestBody IntegrationPlugin plugin) {
        boolean success = pluginService.editPlugin(plugin);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除插件")
    public Result<Void> deletePlugin(@PathVariable String id) {
        boolean success = pluginService.deletePlugin(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @PostMapping("/upload")
    @Operation(summary = "上传插件")
    public Result<IntegrationPlugin> uploadPlugin(
            @Parameter(description = "插件文件", required = true)
            @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(description = "插件名称（可选，默认使用文件名）")
            @RequestParam(value = "pluginName", required = false) String pluginName,
            @Parameter(description = "插件描述")
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @Parameter(description = "插件类型（可选，默认从文件扩展名获取）")
            @RequestParam(value = "pluginType", required = false) String pluginType) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return error("上传文件不能为空");
        }
        
        try {
            IntegrationPlugin plugin = pluginService.uploadPlugin(file, pluginName, description, pluginType);
            return success(plugin);
        } catch (Exception e) {
            return error("上传失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用插件")
    public Result<Void> disablePlugin(@PathVariable String id) {
        try {
            boolean success = pluginService.disablePlugin(id);
            return success ? success("禁用成功") : error("禁用失败");
        } catch (Exception e) {
            return error("禁用失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用插件")
    public Result<Void> enablePlugin(@PathVariable String id) {
        try {
            boolean success = pluginService.enablePlugin(id);
            return success ? success("启用成功") : error("启用失败");
        } catch (Exception e) {
            return error("启用失败：" + e.getMessage());
        }
    }

    @GetMapping("/list-with-status")
    @Operation(summary = "查询插件列表（带状态过滤）")
    public Result<List<IntegrationPlugin>> getPluginListWithStatus(
            String pluginName,
            Integer pluginStatus) {
        List<IntegrationPlugin> result = pluginService.getPluginListWithStatus(pluginName, pluginStatus);
        return success(result);
    }

    @GetMapping("/enabled")
    @Operation(summary = "查询已启用的插件列表")
    public Result<List<IntegrationPlugin>> getEnabledPlugins() {
        List<IntegrationPlugin> result = pluginService.getPluginListWithStatus(null, 1);
        return success(result);
    }

    @PostMapping("/{id}/enable-plugin")
    @Operation(summary = "启用插件（热加载）")
    public Result<Void> enablePluginWithHotload(@PathVariable String id) {
        try {
            boolean success = pluginManager.enablePlugin(id);
            return success ? success("启用成功") : error("启用失败");
        } catch (Exception e) {
            return error("启用失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/disable-plugin")
    @Operation(summary = "禁用插件（热卸载）")
    public Result<Void> disablePluginWithHotload(@PathVariable String id) {
        try {
            boolean success = pluginManager.disablePlugin(id);
            return success ? success("禁用成功") : error("禁用失败");
        } catch (Exception e) {
            return error("禁用失败：" + e.getMessage());
        }
    }

    @GetMapping("/loaded")
    @Operation(summary = "查询已加载的插件列表")
    public Result<List<Map<String, Object>>> getLoadedPlugins() {
        try {
            Map<String, PluginWrapper> loadedPlugins = pluginManager.getLoadedPlugins();
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Map.Entry<String, PluginWrapper> entry : loadedPlugins.entrySet()) {
                PluginWrapper wrapper = entry.getValue();
                Map<String, Object> map = new HashMap<>();
                map.put("pluginId", wrapper.getPluginId());
                map.put("pluginName", wrapper.getPluginName());
                map.put("version", wrapper.getVersion());
                map.put("status", wrapper.getStatus());
                map.put("loadTime", wrapper.getLoadTime());
                result.add(map);
            }
            
            return success(result);
        } catch (Exception e) {
            return error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/invoke")
    @Operation(summary = "调用插件方法")
    public Result<Object> invokePlugin(
            @PathVariable String id,
            @Parameter(description = "方法名", required = true)
            @RequestParam String methodName,
            @Parameter(description = "参数（JSON 格式）")
            @RequestBody(required = false) Object[] args) {
        try {
            Object result = pluginManager.invokePlugin(id, methodName, args);
            return success(result);
        } catch (Exception e) {
            return error("调用失败：" + e.getMessage());
        }
    }
}
