package com.bit.iot.integration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.integration.model.dto.IntegrationConfigListItemDTO;
import com.bit.iot.integration.model.entity.IntegrationConfig;

import java.util.List;

/**
 * <p>
 * 集成实例配置表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
public interface IIntegrationConfigService extends IService<IntegrationConfig> {
    
    /**
     * 分页查询集成配置列表
     * @param page 分页信息
     * @param integrationName 集成实例名称
     * @param pluginId 插件 ID
     * @return 集成配置列表（包含插件名称）
     */
    Page<IntegrationConfigListItemDTO> getIntegrationConfigList(Page<IntegrationConfig> page, String integrationName, String pluginId);
    
    /**
     * 新增集成配置
     * @param integrationConfig 集成配置信息
     * @return 是否成功
     */
    boolean addIntegrationConfig(IntegrationConfig integrationConfig);
    
    /**
     * 编辑集成配置
     * @param integrationConfig 集成配置信息
     * @return 是否成功
     */
    boolean editIntegrationConfig(IntegrationConfig integrationConfig);
    
    /**
     * 删除集成配置
     * @param id 集成配置 ID
     * @return 是否成功
     */
    boolean deleteIntegrationConfig(String id);
    
    /**
     * 根据插件 ID 查询集成配置列表
     * @param pluginId 插件 ID
     * @return 集成配置列表
     */
    List<IntegrationConfig> getIntegrationConfigsByPluginId(String pluginId);
    
    /**
     * 根据 ID 查询单个集成配置
     * @param id 集成配置 ID
     * @return 集成配置信息
     */
    IntegrationConfig getIntegrationConfigById(String id);
    
    /**
     * 启动集成配置
     * @param id 集成配置 ID
     * @return 是否成功
     */
    boolean startIntegration(String id);
    
    /**
     * 暂停集成配置
     * @param id 集成配置 ID
     * @return 是否成功
     */
    boolean pauseIntegration(String id);
    
}
