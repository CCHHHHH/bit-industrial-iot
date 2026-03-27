package com.bit.iot.integration.plugin;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 插件包装器
 * 包含插件实例和相关信息
 *
 * @author chenhao
 * @since 2026-03-20
 */
@Getter
@Setter
public class PluginWrapper {
    
    /**
     * 插件 ID
     */
    private String pluginId;
    
    /**
     * 插件名称
     */
    private String pluginName;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件描述
     */
    private String description;
    
    /**
     * 插件路径
     */
    private String pluginPath;
    
    /**
     * 插件状态（0-禁用，1-启用）
     */
    private Integer status;
    
    /**
     * 插件实例
     */
    private IPlugin pluginInstance;
    
    /**
     * 类加载器
     */
    private PluginClassLoader classLoader;
    
    /**
     * 加载时间
     */
    private Date loadTime;
    
    /**
     * 最后修改时间
     */
    private Date lastModifiedTime;
    
    public PluginWrapper() {
        this.loadTime = new Date();
    }
    
    /**
     * 初始化插件
     */
    public void init() {
        if (pluginInstance != null) {
            pluginInstance.init();
        }
    }
    
    /**
     * 启动插件
     */
    public void start() {
        if (pluginInstance != null) {
            pluginInstance.start();
        }
    }
    
    /**
     * 停止插件
     */
    public void stop() {
        if (pluginInstance != null) {
            pluginInstance.stop();
        }
    }
    
    /**
     * 销毁插件
     */
    public void destroy() {
        if (pluginInstance != null) {
            pluginInstance.destroy();
        }
    }
    
    /**
     * 执行插件方法
     * @param methodName 方法名
     * @param args 参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public Object execute(String methodName, Object... args) throws Exception {
        if (pluginInstance == null) {
            throw new IllegalStateException("插件未加载：" + pluginName);
        }
        return pluginInstance.execute(methodName, args);
    }
}
