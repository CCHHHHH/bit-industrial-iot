package com.bit.iot.integration.plugin;

import com.bit.iot.integration.base.IPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

import com.bit.iot.integration.model.dto.PluginConfigItemDTO;

/**
 * 插件加载器
 * 负责加载、卸载和热替换插件
 *
 * @author chenhao
 * @since 2026-03-20
 */
@Slf4j
@Component
public class PluginLoader {
    
    /**
     * 插件缓存：pluginId -> PluginWrapper
     */
    private final Map<String, PluginWrapper> pluginCache = new ConcurrentHashMap<>();
    
    /**
     * 加载插件
     * @param pluginId 插件 ID
     * @param pluginPath 插件路径
     * @return 插件包装器
     * @throws Exception 加载异常
     */
    public PluginWrapper loadPlugin(String pluginId, String pluginPath) throws Exception {
        log.info("开始加载插件：{}, 路径：{}", pluginId, pluginPath);
        
        // 检查文件是否存在
        File pluginFile = new File(pluginPath).getCanonicalFile();
        if (!pluginFile.exists()) {
            throw new IllegalArgumentException("插件文件不存在：" + pluginPath);
        }
        if (!pluginFile.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("插件文件必须是 .jar: " + pluginPath);
        }
        
        // 如果已经加载，先卸载
        if (pluginCache.containsKey(pluginId)) {
            unloadPlugin(pluginId);
        }
        
        // 创建类加载器
        URL pluginUrl = pluginFile.toURI().toURL();
        PluginClassLoader classLoader = new PluginClassLoader(
            pluginId, 
            pluginFile.getName(),
            new URL[]{pluginUrl}, 
            Thread.currentThread().getContextClassLoader()
        );
        
        // 加载插件主类（假设插件实现类名为 PluginImpl）
        Class<?> pluginClass = classLoader.loadClass("com.bit.iot.plugin.PluginImpl");
        
        // 检查是否实现了 IPlugin 接口
        if (!IPlugin.class.isAssignableFrom(pluginClass)) {
            throw new IllegalArgumentException("插件类必须实现 IPlugin 接口");
        }
        
        // 创建插件实例
        IPlugin pluginInstance = (IPlugin) pluginClass.getDeclaredConstructor().newInstance();
        
        // 创建插件包装器
        PluginWrapper wrapper = new PluginWrapper();
        wrapper.setPluginId(pluginId);
        wrapper.setPluginName(pluginInstance.getName());
        wrapper.setVersion(pluginInstance.getVersion());
        wrapper.setPluginInstance(pluginInstance);
        wrapper.setClassLoader(classLoader);
        wrapper.setPluginPath(pluginPath);
        wrapper.setLastModifiedTime(new Date(pluginFile.lastModified()));
        wrapper.setConfigResourcePath(pluginInstance.getConfigResourcePath());
        
        // 初始化插件
        wrapper.init();
        
        // 启动插件
        wrapper.start();
        
        // 缓存插件
        pluginCache.put(pluginId, wrapper);
        
        log.info("插件加载成功：{} (版本：{})", wrapper.getPluginName(), wrapper.getVersion());
        
        return wrapper;
    }
    
    /**
     * 卸载插件
     * @param pluginId 插件 ID
     * @return 是否成功
     */
    public boolean unloadPlugin(String pluginId) {
        log.info("开始卸载插件：{}", pluginId);
        
        PluginWrapper wrapper = pluginCache.remove(pluginId);
        if (wrapper == null) {
            log.warn("插件未找到：{}", pluginId);
            return false;
        }
        
        try {
            // 停止插件
            wrapper.stop();
            
            // 销毁插件
            wrapper.destroy();
            
            // 关闭类加载器
            if (wrapper.getClassLoader() != null) {
                wrapper.getClassLoader().close();
            }
            
            // 帮助 GC 回收
            wrapper.setPluginInstance(null);
            wrapper.setClassLoader(null);
            
            log.info("插件卸载成功：{}", pluginId);
            return true;
            
        } catch (Exception e) {
            log.error("卸载插件失败：{}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 热替换插件
     * @param pluginId 插件 ID
     * @param newPluginPath 新插件路径
     * @return 插件包装器
     * @throws Exception 替换异常
     */
    public PluginWrapper reloadPlugin(String pluginId, String newPluginPath) throws Exception {
        log.info("开始热替换插件：{}, 新路径：{}", pluginId, newPluginPath);
        
        // 卸载旧插件
        unloadPlugin(pluginId);
        
        // 加载新插件
        return loadPlugin(pluginId, newPluginPath);
    }
    
    /**
     * 获取插件
     * @param pluginId 插件 ID
     * @return 插件包装器
     */
    public PluginWrapper getPlugin(String pluginId) {
        return pluginCache.get(pluginId);
    }
    
    /**
     * 获取所有已加载的插件
     * @return 插件列表
     */
    public Map<String, PluginWrapper> getAllPlugins() {
        return new ConcurrentHashMap<>(pluginCache);
    }

    public List<PluginConfigItemDTO> readDefaultConfig(String pluginPath) throws Exception {
        File pluginFile = new File(pluginPath).getCanonicalFile();
        if (!pluginFile.exists()) {
            throw new IllegalArgumentException("插件文件不存在：" + pluginPath);
        }
        if (!pluginFile.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("插件文件必须是 .jar: " + pluginPath);
        }

        String configResourcePath = resolveConfigResourcePath(pluginFile);

        try (JarFile jarFile = new JarFile(pluginFile)) {
            var entry = jarFile.getJarEntry(configResourcePath);
            if (entry == null) {
                throw new IllegalStateException("插件默认配置文件不存在：" + configResourcePath);
            }
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                return parseYamlToConfigItems(inputStream);
            }
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
    public Object invokePluginMethod(String pluginId, String methodName, Object... args) throws Exception {
        PluginWrapper wrapper = getPlugin(pluginId);
        if (wrapper == null) {
            throw new IllegalStateException("插件未找到：" + pluginId);
        }
        
        if (wrapper.getStatus() != null && wrapper.getStatus() == 0) {
            throw new IllegalStateException("插件已禁用：" + pluginId);
        }
        
        log.debug("调用插件方法：{}.{}({})", pluginId, methodName, args);
        
        return wrapper.execute(methodName, args);
    }
    
    /**
     * 检查插件是否需要更新
     * @param pluginId 插件 ID
     * @return 是否需要更新
     */
    public boolean needsReload(String pluginId, String pluginPath) {
        PluginWrapper wrapper = getPlugin(pluginId);
        if (wrapper == null) {
            return false;
        }
        
        File pluginFile = new File(pluginPath);
        if (!pluginFile.exists()) {
            return false;
        }
        
        long lastModified = pluginFile.lastModified();
        return lastModified > wrapper.getLastModifiedTime().getTime();
    }
    
    /**
     * 关闭所有插件
     */
    public void shutdownAll() {
        log.info("正在关闭所有插件...");
        
        for (String pluginId : pluginCache.keySet()) {
            try {
                unloadPlugin(pluginId);
            } catch (Exception e) {
                log.error("关闭插件失败：{}", pluginId, e);
            }
        }
        
        pluginCache.clear();
        log.info("所有插件已关闭");
    }

    private List<PluginConfigItemDTO> parseYamlToConfigItems(InputStream inputStream) {
        Object loaded = new Yaml().load(inputStream);
        List<PluginConfigItemDTO> result = new ArrayList<>();
        if (loaded instanceof Map<?, ?> rawMap) {
            Object configItems = rawMap.get("config");
            if (configItems instanceof List<?> configList) {
                for (Object item : configList) {
                    if (!(item instanceof Map<?, ?> itemMap)) {
                        continue;
                    }
                    PluginConfigItemDTO dto = new PluginConfigItemDTO();
                    dto.setKey(stringValue(itemMap.get("key")));
                    dto.setValue(stringValue(itemMap.get("value")));
                    dto.setDescription(stringValue(itemMap.get("description")));
                    if (dto.getKey() != null && !dto.getKey().isEmpty()) {
                        result.add(dto);
                    }
                }
                return result;
            }

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                PluginConfigItemDTO dto = new PluginConfigItemDTO();
                dto.setKey(String.valueOf(entry.getKey()));
                if (entry.getValue() instanceof Map<?, ?> valueMap) {
                    dto.setValue(stringValue(valueMap.get("value")));
                    dto.setDescription(stringValue(valueMap.get("description")));
                } else {
                    dto.setValue(stringValue(entry.getValue()));
                    dto.setDescription("");
                }
                result.add(dto);
            }
        }
        return result;
    }

    private String resolveConfigResourcePath(File pluginFile) throws Exception {
        URL pluginUrl = pluginFile.toURI().toURL();
        try (PluginClassLoader classLoader = new PluginClassLoader(
                "config-probe",
                pluginFile.getName(),
                new URL[]{pluginUrl},
                Thread.currentThread().getContextClassLoader())) {
            Class<?> pluginClass = classLoader.loadClass("com.bit.iot.plugin.PluginImpl");
            if (!IPlugin.class.isAssignableFrom(pluginClass)) {
                throw new IllegalArgumentException("插件类必须实现 IPlugin 接口");
            }
            IPlugin pluginInstance = (IPlugin) pluginClass.getDeclaredConstructor().newInstance();
            return pluginInstance.getConfigResourcePath();
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
