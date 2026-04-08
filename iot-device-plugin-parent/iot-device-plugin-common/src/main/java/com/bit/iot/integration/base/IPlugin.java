package com.bit.iot.integration.base;

import java.util.Map;

/**
 * 插件接口定义
 * 所有插件必须实现此接口
 */
public interface IPlugin {

    String getName();

    String getVersion();

    void init();

    void start();

    void stop();

    void destroy();

    Object execute(String methodName, Object... args) throws Exception;

    default void startInstance(String integrationId, Object config) throws Exception {
        // 默认不实现，由插件选择性重写
    }

    default void stopInstance(String integrationId) throws Exception {
        // 默认不实现，由插件选择性重写
    }

    default Object handleDeviceProperty(String sourceData) throws Exception {
        return null;
    }

    default Object handleDeviceStatus(String sourceData) throws Exception {
        return null;
    }

    default Object handleTimeSeriesData(String sourceData) throws Exception {
        return null;
    }

    default void loadConfig(Map<String, String> config) {
        // 默认不实现，由插件选择性重写
    }

    default String getConfigResourcePath() {
        return "plugin-config.yml";
    }
}
