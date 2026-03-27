package com.bit.iot.integration.plugin;

/**
 * 插件基类
 * 提供通用的实现，简化插件开发
 *
 * @author chenhao
 * @since 2026-03-20
 */
public abstract class BasePlugin implements IPlugin {
    
    /**
     * 插件名称
     */
    protected String name;
    
    /**
     * 插件版本
     */
    protected String version;
    
    public BasePlugin(String name, String version) {
        this.name = name;
        this.version = version;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public void init() {
        // 默认不做任何操作，子类可以重写
    }
    
    @Override
    public void start() {
        // 默认不做任何操作，子类可以重写
    }
    
    @Override
    public void stop() {
        // 默认不做任何操作，子类可以重写
    }
    
    @Override
    public void destroy() {
        // 默认不做任何操作，子类可以重写
    }

    @Override
    public Object handleDeviceProperty(String sourceData) throws Exception {
        return null;
    }

    @Override
    public Object handleDeviceStatus(String sourceData) throws Exception {
        return null;
    }

    @Override
    public Object handleTimeSeriesData(String sourceData) throws Exception {
        return null;
    }
}
