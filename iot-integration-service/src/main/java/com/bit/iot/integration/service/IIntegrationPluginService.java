package com.bit.iot.integration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.integration.model.entity.IntegrationPlugin;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 集成管理插件表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
public interface IIntegrationPluginService extends IService<IntegrationPlugin> {
    
    /**
     * 分页查询插件列表
     * @param page 分页信息
     * @param pluginName 插件名称
     * @return 插件列表
     */
    Page<IntegrationPlugin> getPluginList(Page<IntegrationPlugin> page, String pluginName);
    
    /**
     * 查询插件列表（带状态过滤）
     * @param pluginName 插件名称
     * @param pluginStatus 插件状态（0-禁用，1-启用，null-全部）
     * @return 插件列表
     */
    List<IntegrationPlugin> getPluginListWithStatus(String pluginName, Integer pluginStatus);
    
    /**
     * 新增插件
     * @param plugin 插件信息
     * @return 是否成功
     */
    boolean addPlugin(IntegrationPlugin plugin);
    
    /**
     * 编辑插件
     * @param plugin 插件信息
     * @return 是否成功
     */
    boolean editPlugin(IntegrationPlugin plugin);
    
    /**
     * 删除插件
     * @param id 插件 ID
     * @return 是否成功
     */
    boolean deletePlugin(String id);
    
    /**
     * 上传插件
     * @param file 插件文件
     * @param pluginName 插件名称（可选，为空则使用文件名）
     * @param description 插件描述
     * @param pluginType 插件类型（可选，为空则从文件扩展名获取）
     * @return 插件信息
     * @throws Exception 上传异常
     */
    IntegrationPlugin uploadPlugin(MultipartFile file, String pluginName, String description, String pluginType) throws Exception;
    
    /**
     * 禁用插件
     * @param id 插件 ID
     * @return 是否成功
     */
    boolean disablePlugin(String id);
    
    /**
     * 启用插件
     * @param id 插件 ID
     * @return 是否成功
     */
    boolean enablePlugin(String id);
    
}
