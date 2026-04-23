package com.bit.iot.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.integration.model.dto.IntegrationConfigListItemDTO;
import com.bit.iot.integration.model.entity.IntegrationConfig;
import com.bit.iot.integration.model.entity.IntegrationPlugin;
import com.bit.iot.integration.dao.IntegrationConfigMapper;
import com.bit.iot.integration.plugin.PluginManager;
import com.bit.iot.integration.scheduler.IntegrationCollectScheduler;
import com.bit.iot.integration.service.IIntegrationConfigService;
import com.bit.iot.integration.service.IIntegrationPluginService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 集成实例配置表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@Slf4j
@Service
public class IntegrationConfigServiceImpl extends ServiceImpl<IntegrationConfigMapper, IntegrationConfig> implements IIntegrationConfigService {

    @Autowired
    private PluginManager pluginManager;
    
    @Autowired
    private IIntegrationPluginService pluginService;

    @Autowired
    @Lazy
    private IntegrationCollectScheduler collectScheduler;

    @Override
    public Page<IntegrationConfigListItemDTO> getIntegrationConfigList(Page<IntegrationConfig> page, String integrationName, String pluginId) {
        QueryWrapper<IntegrationConfig> queryWrapper = new QueryWrapper<>();
        if (integrationName != null && !integrationName.isEmpty()) {
            queryWrapper.like("integration_name", integrationName);
        }
        if (pluginId != null && !pluginId.isEmpty()) {
            queryWrapper.eq("plugin_id", pluginId);
        }
        queryWrapper.orderByDesc("create_time");
        
        // 查询集成配置列表
        Page<IntegrationConfig> configPage = this.page(page, queryWrapper);
        
        // 获取所有相关的插件 ID
        List<String> pluginIds = configPage.getRecords().stream()
                .map(IntegrationConfig::getPluginId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询插件信息
        Map<String, String> pluginNameMap = new HashMap<>();
        if (!pluginIds.isEmpty()) {
            QueryWrapper<IntegrationPlugin> pluginQueryWrapper = new QueryWrapper<>();
            pluginQueryWrapper.in("id", pluginIds);
            List<IntegrationPlugin> plugins = pluginService.list(pluginQueryWrapper);
            
            for (IntegrationPlugin plugin : plugins) {
                pluginNameMap.put(plugin.getId(), plugin.getPluginName());
            }
        }
        
        // 组装返回结果
        List<IntegrationConfigListItemDTO> dtoList = new ArrayList<>();
        for (IntegrationConfig config : configPage.getRecords()) {
            IntegrationConfigListItemDTO dto = new IntegrationConfigListItemDTO();
            dto.setId(config.getId());
            dto.setIntegrationName(config.getIntegrationName());
            dto.setPluginId(config.getPluginId());
            dto.setPluginName(pluginNameMap.get(config.getPluginId()));
            dto.setIntegrationStatus(config.getIntegrationStatus());
            dto.setIntegrationDesc(config.getIntegrationDesc());
            dto.setCreateTime(config.getCreateTime());
            dto.setUpdateTime(config.getUpdateTime());
            dtoList.add(dto);
        }
        
        // 创建新的分页对象
        Page<IntegrationConfigListItemDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), configPage.getTotal());
        resultPage.setRecords(dtoList);
        
        return resultPage;
    }
    
    @Override
    public boolean addIntegrationConfig(IntegrationConfig integrationConfig) {
        // 重名校验
        long count = this.count(new QueryWrapper<IntegrationConfig>()
                .eq("integration_name", integrationConfig.getIntegrationName()));
        if (count > 0) {
            throw new RuntimeException("集成实例名称已存在：" + integrationConfig.getIntegrationName());
        }
        Date now = new Date();
        integrationConfig.setCreateTime(now);
        integrationConfig.setUpdateTime(now);
        return this.save(integrationConfig);
    }
    
    @Override
    public boolean editIntegrationConfig(IntegrationConfig integrationConfig) {
        Date now = new Date();
        integrationConfig.setUpdateTime(now);
        return this.updateById(integrationConfig);
    }
    
    @Override
    public boolean deleteIntegrationConfig(String id) {
        return this.removeById(id);
    }
    
    @Override
    public List<IntegrationConfig> getIntegrationConfigsByPluginId(String pluginId) {
        QueryWrapper<IntegrationConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("plugin_id", pluginId);
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public IntegrationConfig getIntegrationConfigById(String id) {
        return this.getById(id);
    }

    @Override
    public boolean startIntegration(String id) {
        IntegrationConfig config = this.getById(id);
        if (config == null) {
            throw new IllegalArgumentException("集成配置不存在");
        }
        
        // 检查是否已加载插件
        String pluginId = config.getPluginId();
        if (pluginId == null || pluginId.isEmpty()) {
            throw new IllegalStateException("集成配置未关联插件");
        }
        
        // 检查插件是否启用
        IntegrationPlugin plugin = pluginService.getById(pluginId);
        if (plugin == null) {
            throw new IllegalStateException("插件不存在：" + pluginId);
        }
        if (plugin.getPluginStatus() == null || plugin.getPluginStatus() == 0) {
            throw new IllegalStateException("插件已禁用，无法启动集成实例：" + plugin.getPluginName());
        }
        
        // 调用插件的启动方法
        try {
            log.info("开始调用插件启动集成实例：pluginId={}, integrationId={}", pluginId, id);
            pluginManager.startIntegrationInstance(pluginId, id, config);
            log.info("插件启动成功，更新数据库状态");
        } catch (Exception e) {
            log.error("调用插件启动失败：pluginId={}, integrationId={}", pluginId, id, e);
            throw new RuntimeException("启动集成实例失败：" + e.getMessage(), e);
        }
        
        // 更新数据库状态
        config.setIntegrationStatus(1); // 1 表示运行中
        config.setUpdateTime(new Date());
        boolean updated = this.updateById(config);
        if (updated) {
            collectScheduler.startIntegration(id);
        }
        return updated;
    }

    @Override
    public boolean pauseIntegration(String id) {
        IntegrationConfig config = this.getById(id);
        if (config == null) {
            throw new IllegalArgumentException("集成配置不存在");
        }
        
        // 检查是否已加载插件
        String pluginId = config.getPluginId();
        if (pluginId == null || pluginId.isEmpty()) {
            throw new IllegalStateException("集成配置未关联插件");
        }
        
        // 调用插件的停止方法
        try {
            log.info("开始调用插件停止集成实例：pluginId={}, integrationId={}", pluginId, id);
            pluginManager.stopIntegrationInstance(pluginId, id);
            log.info("插件停止成功，更新数据库状态");
        } catch (Exception e) {
            log.error("调用插件停止失败：pluginId={}, integrationId={}", pluginId, id, e);
            // 即使插件停止失败，也更新数据库状态
        }
        
        // 更新数据库状态
        config.setIntegrationStatus(0); // 0 表示停用
        config.setUpdateTime(new Date());
        boolean updated = this.updateById(config);
        collectScheduler.stopIntegration(id);
        return updated;
    }

}
