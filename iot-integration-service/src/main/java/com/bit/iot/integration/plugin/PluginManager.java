package com.bit.iot.integration.plugin;

import com.bit.iot.integration.model.entity.IntegrationConfig;
import com.bit.iot.integration.model.entity.IntegrationPlugin;
import com.bit.iot.integration.service.IIntegrationConfigService;
import com.bit.iot.integration.service.IIntegrationConfigParamService;
import com.bit.iot.integration.service.IIntegrationPluginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;

/**
 * 插件管理器
 * 管理插件的生命周期，支持热加载和自动扫描
 *
 * @author chenhao
 * @since 2026-03-20
 */
@Slf4j
@Component
public class PluginManager {
    
    @Autowired
    private IIntegrationPluginService pluginService;
    
    @Autowired
    private PluginLoader pluginLoader;
    
    @Autowired
    @Lazy
    private IIntegrationConfigService integrationConfigService;

    @Autowired
    private IIntegrationConfigParamService integrationConfigParamService;
    
    /**
     * 应用启动完成后加载已启用的插件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("初始化插件管理器...");
        
        try {
            // 按照正确流程处理：以集成实例为中心
            autoStartRunningIntegrations();
            
            log.info("插件管理器初始化完成，当前运行 {} 个插件", pluginLoader.getAllPlugins().size());
            
        } catch (Exception e) {
            log.error("初始化插件管理器失败", e);
        }
    }
    
    /**
     * 自动启动已启用集成实例绑定的插件
     * 流程：
     * 1. 查询数据库中所有启动的集成实例
     * 2. 查询启动的集成实例对应的插件
     * 3. 判断插件是否是禁用状态
     * 4. 如果是启用状态则加载该插件并调用启动方法
     */
    private void autoStartRunningIntegrations() {
        log.info("开始自动启动已启用集成实例绑定的插件...");
        
        try {
            // 步骤 1: 查询数据库中所有启动的集成实例
            List<IntegrationConfig> runningConfigs = integrationConfigService.list();
            
            for (IntegrationConfig config : runningConfigs) {
                // 只处理已启动的集成实例 (status=1)
                if (config.getIntegrationStatus() == null || config.getIntegrationStatus() != 1) {
                    continue;
                }
                
                String pluginId = config.getPluginId();
                if (pluginId == null || pluginId.isEmpty()) {
                    log.warn("集成实例未绑定插件，跳过：{}", config.getId());
                    continue;
                }
                
                // 步骤 2: 查询启动的集成实例对应的插件
                IntegrationPlugin plugin = pluginService.getById(pluginId);
                if (plugin == null) {
                    log.error("集成实例绑定的插件不存在：integrationId={}, pluginId={}", 
                        config.getId(), pluginId);
                    continue;
                }
                
                // 步骤 3: 判断插件是否是禁用状态
                if (plugin.getPluginStatus() == null || plugin.getPluginStatus() == 0) {
                    log.warn("集成实例绑定的插件已禁用，跳过启动：integrationId={}, pluginName={}", 
                        config.getId(), plugin.getPluginName());
                    continue;
                }
                
                // 步骤 4: 如果是启用状态则加载该插件并调用启动方法
                try {
                    // 检查插件是否已加载，未加载则加载
                    PluginWrapper wrapper = pluginLoader.getPlugin(pluginId);
                    if (wrapper == null) {
                        log.info("加载插件：{}", plugin.getPluginName());
                        pluginLoader.loadPlugin(pluginId, plugin.getPluginPath());
                    }
                    
                    // 调用插件的启动方法
                    log.info("启动集成实例：{} (绑定插件：{})", config.getIntegrationName(), pluginId);
                    startIntegrationInstance(pluginId, config.getId(), config);
                    log.info("集成实例已自动启动：{}", config.getId());
                    
                } catch (Exception e) {
                    log.error("加载或启动插件失败：integrationId={}, pluginName={}", 
                        config.getId(), plugin.getPluginName(), e);
                }
            }
            
            log.info("已启用集成实例自动启动检查完成");
            
        } catch (Exception e) {
            log.error("自动启动集成实例检查失败", e);
        }
    }
    
    /**
     * 定时检查插件更新（每 5 分钟）
     */
    @Scheduled(fixedRate = 300000)
    public void checkPluginUpdates() {
        log.debug("开始检查插件更新...");
        
        Map<String, PluginWrapper> loadedPlugins = pluginLoader.getAllPlugins();
        
        for (Map.Entry<String, PluginWrapper> entry : loadedPlugins.entrySet()) {
            String pluginId = entry.getKey();
            PluginWrapper wrapper = entry.getValue();
            
            try {
                // 从数据库获取最新信息
                IntegrationPlugin plugin = pluginService.getById(pluginId);
                if (plugin != null && plugin.getPluginStatus() == 1) {
                    // 检查文件是否被修改
                    if (pluginLoader.needsReload(pluginId, plugin.getPluginPath())) {
                        log.info("检测到插件更新，开始热替换：{}", plugin.getPluginName());
                        pluginLoader.reloadPlugin(pluginId, plugin.getPluginPath());
                    }
                }
            } catch (Exception e) {
                log.error("检查插件更新失败：{}", pluginId, e);
            }
        }
    }
    
    /**
     * 启用插件
     * @param pluginId 插件 ID
     * @return 是否成功
     */
    public boolean enablePlugin(String pluginId) {
        try {
            IntegrationPlugin plugin = pluginService.getById(pluginId);
            if (plugin == null) {
                log.error("插件不存在：{}", pluginId);
                return false;
            }
            
            // 加载插件
            pluginLoader.loadPlugin(pluginId, plugin.getPluginPath());
            
            // 更新数据库状态
            plugin.setPluginStatus(1);
            pluginService.updateById(plugin);
            
            log.info("插件已启用：{}", plugin.getPluginName());
            return true;
            
        } catch (Exception e) {
            log.error("启用插件失败：{}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 禁用插件
     * @param pluginId 插件 ID
     * @return 是否成功
     */
    public boolean disablePlugin(String pluginId) {
        try {
            // 卸载插件
            pluginLoader.unloadPlugin(pluginId);
            
            // 更新数据库状态
            IntegrationPlugin plugin = pluginService.getById(pluginId);
            if (plugin != null) {
                plugin.setPluginStatus(0);
                pluginService.updateById(plugin);
            }
            
            log.info("插件已禁用：{}", pluginId);
            return true;
            
        } catch (Exception e) {
            log.error("禁用插件失败：{}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 调用插件方法
     * @param pluginId 插件 ID
     * @param methodName 方法名
     * @param args 参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public Object invokePlugin(String pluginId, String methodName, Object... args) throws Exception {
        PluginWrapper wrapper = pluginLoader.getPlugin(pluginId);
        if (wrapper == null || wrapper.getPluginInstance() == null) {
            throw new IllegalStateException("插件未加载：" + pluginId);
        }
        return switch (methodName) {
            case "handleDeviceProperty" -> wrapper.getPluginInstance().handleDeviceProperty((String) args[0]);
            case "handleDeviceStatus" -> wrapper.getPluginInstance().handleDeviceStatus((String) args[0]);
            case "handleTimeSeriesData" -> wrapper.getPluginInstance().handleTimeSeriesData((String) args[0]);
            default -> throw new IllegalArgumentException("禁止调用未授权的插件方法: " + methodName);
        };
    }
    
    /**
     * 启动插件中的集成实例
     * @param pluginId 插件 ID
     * @param integrationId 集成配置 ID
     * @param config 集成配置信息
     * @throws Exception 执行异常
     */
    public void startIntegrationInstance(String pluginId, String integrationId, IntegrationConfig config) throws Exception {
        PluginWrapper wrapper = pluginLoader.getPlugin(pluginId);
        if (wrapper == null) {
            throw new IllegalStateException("插件未加载：" + pluginId);
        }
        
        if (wrapper.getStatus() != null && wrapper.getStatus() == 0) {
            throw new IllegalStateException("插件已禁用：" + pluginId);
        }
        
        log.info("调用插件启动方法：{}.startInstance({}, {})", pluginId, integrationId, config);
        
        // 调用插件的 startInstance 方法
        try {
            wrapper.getPluginInstance().loadConfig(integrationConfigParamService.getConfigParamMap(integrationId));
            wrapper.getPluginInstance().startInstance(integrationId, config);
            log.info("插件启动成功：pluginId={}, integrationId={}", pluginId, integrationId);
        } catch (Exception e) {
            log.error("插件启动失败：pluginId={}, integrationId={}", pluginId, integrationId, e);
            throw e;
        }
    }
    
    /**
     * 停止插件中的集成实例
     * @param pluginId 插件 ID
     * @param integrationId 集成配置 ID
     * @throws Exception 执行异常
     */
    public void stopIntegrationInstance(String pluginId, String integrationId) throws Exception {
        PluginWrapper wrapper = pluginLoader.getPlugin(pluginId);
        if (wrapper == null) {
            throw new IllegalStateException("插件未加载：" + pluginId);
        }
        
        log.info("调用插件停止方法：{}.stopInstance({})", pluginId, integrationId);
        
        // 调用插件的 stopInstance 方法
        try {
            wrapper.getPluginInstance().stopInstance(integrationId);
            log.info("插件停止成功：pluginId={}, integrationId={}", pluginId, integrationId);
        } catch (Exception e) {
            log.error("插件停止失败：pluginId={}, integrationId={}", pluginId, integrationId, e);
            throw e;
        }
    }
    
    /**
     * 获取所有已加载的插件
     * @return 插件列表
     */
    public Map<String, PluginWrapper> getLoadedPlugins() {
        return pluginLoader.getAllPlugins();
    }

    public java.util.List<com.bit.iot.integration.model.dto.PluginConfigItemDTO> readDefaultConfig(String pluginId, String pluginPath) throws Exception {
        return pluginLoader.readDefaultConfig(pluginPath);
    }
    
    /**
     * 应用关闭时清理所有插件
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭插件管理器...");
        pluginLoader.shutdownAll();
        log.info("插件管理器已关闭");
    }
}
