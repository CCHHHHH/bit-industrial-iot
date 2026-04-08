package com.bit.iot.integration.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件基类
 * 提供通用的实现，简化插件开发
 */
public abstract class BasePlugin implements IPlugin {

    protected String name;

    protected String version;

    protected final Map<String, String> currentConfig = new HashMap<>();

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

    @Override
    public void loadConfig(Map<String, String> config) {
        currentConfig.clear();
        if (config != null) {
            currentConfig.putAll(config);
        }
    }

    public Map<String, String> getCurrentConfig() {
        return Collections.unmodifiableMap(currentConfig);
    }

    protected String getConfig(String key) {
        return currentConfig.get(key);
    }

    protected String getConfigOrDefault(String key, String defaultValue) {
        return currentConfig.getOrDefault(key, defaultValue);
    }
}
