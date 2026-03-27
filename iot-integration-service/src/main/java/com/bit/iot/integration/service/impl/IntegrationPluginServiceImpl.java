package com.bit.iot.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.integration.dao.IntegrationPluginMapper;
import com.bit.iot.integration.model.entity.IntegrationPlugin;
import com.bit.iot.integration.service.IIntegrationPluginService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 集成管理插件表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@Service
public class IntegrationPluginServiceImpl extends ServiceImpl<IntegrationPluginMapper, IntegrationPlugin> implements IIntegrationPluginService {

    @Value("${plugin.upload.path:./plugins}")
    private String pluginUploadPath;

    /**
     * 默认插件版本
     */
    private static final String DEFAULT_PLUGIN_VERSION = "1.0.0";

    /**
     * 默认插件状态（启用）
     */
    private static final int DEFAULT_PLUGIN_STATUS = 1;

    @Override
    public Page<IntegrationPlugin> getPluginList(Page<IntegrationPlugin> page, String pluginName) {
        QueryWrapper<IntegrationPlugin> queryWrapper = new QueryWrapper<>();
        if (pluginName != null && !pluginName.isEmpty()) {
            queryWrapper.like("plugin_name", pluginName);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }
    
    @Override
    public boolean addPlugin(IntegrationPlugin plugin) {
        // 重名校验
        long count = this.count(new QueryWrapper<IntegrationPlugin>()
                .eq("plugin_name", plugin.getPluginName()));
        if (count > 0) {
            throw new RuntimeException("插件名称已存在：" + plugin.getPluginName());
        }
        Date now = new Date();
        plugin.setCreateTime(now);
        plugin.setUpdateTime(now);
        return this.save(plugin);
    }
    
    @Override
    public boolean editPlugin(IntegrationPlugin plugin) {
        Date now = new Date();
        plugin.setUpdateTime(now);
        return this.updateById(plugin);
    }
    
    @Override
    public boolean deletePlugin(String id) {
        return this.removeById(id);
    }

    @Override
    public IntegrationPlugin uploadPlugin(MultipartFile file, String pluginName, String description, String pluginType) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 如果未提供插件名称，从文件名中获取（去除扩展名）
        if (pluginName == null || pluginName.trim().isEmpty()) {
            int dotIndex = originalFilename.lastIndexOf('.');
            pluginName = dotIndex > 0 ? originalFilename.substring(0, dotIndex) : originalFilename;
        }

        // 获取文件扩展名
        int dotIndex = originalFilename.lastIndexOf('.');
        String fileExtension = dotIndex > 0 ? originalFilename.substring(dotIndex) : "";
        
        // 如果未提供插件类型，使用文件扩展名
        if (pluginType == null || pluginType.trim().isEmpty()) {
            pluginType = fileExtension;
        }

        // 重名校验
        long count = this.count(new QueryWrapper<IntegrationPlugin>()
                .eq("plugin_name", pluginName));
        if (count > 0) {
            throw new RuntimeException("插件名称已存在：" + pluginName);
        }

        // 生成唯一的文件名，避免冲突
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // 创建上传目录
        Path uploadPath = Paths.get(pluginUploadPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 保存文件
        Path filePath = uploadPath.resolve(uniqueFilename);
        File destFile = filePath.toFile();
        file.transferTo(destFile);

        // 构建插件路径（相对路径或绝对路径）
        String pluginPath = filePath.toAbsolutePath().toString();

        // 获取文件大小
        long fileSize = file.getSize();

        // 创建插件实体
        IntegrationPlugin plugin = new IntegrationPlugin();
        plugin.setPluginName(pluginName);
        plugin.setPluginDescription(description);
        plugin.setPluginPath(pluginPath);
        plugin.setPluginType(pluginType);
        plugin.setPluginStatus(DEFAULT_PLUGIN_STATUS);
        plugin.setPluginVersion(DEFAULT_PLUGIN_VERSION);
        plugin.setPluginSize((int) fileSize);
        Date now = new Date();
        plugin.setCreateTime(now);
        plugin.setUpdateTime(now);

        // 保存到数据库
        this.save(plugin);

        return plugin;
    }

    @Override
    public List<IntegrationPlugin> getPluginListWithStatus(String pluginName, Integer pluginStatus) {
        QueryWrapper<IntegrationPlugin> queryWrapper = new QueryWrapper<>();
        if (pluginName != null && !pluginName.isEmpty()) {
            queryWrapper.like("plugin_name", pluginName);
        }
        if (pluginStatus != null) {
            queryWrapper.eq("plugin_status", pluginStatus);
        }
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public boolean disablePlugin(String id) {
        IntegrationPlugin plugin = this.getById(id);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在");
        }
        plugin.setPluginStatus(0); // 0 表示禁用
        plugin.setUpdateTime(new Date());
        return this.updateById(plugin);
    }

    @Override
    public boolean enablePlugin(String id) {
        IntegrationPlugin plugin = this.getById(id);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在");
        }
        plugin.setPluginStatus(1); // 1 表示启用
        plugin.setUpdateTime(new Date());
        return this.updateById(plugin);
    }

}
