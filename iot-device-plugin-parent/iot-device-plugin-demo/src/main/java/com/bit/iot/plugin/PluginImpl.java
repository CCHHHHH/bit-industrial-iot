package com.bit.iot.plugin;

import com.bit.iot.integration.base.BasePlugin;
import lombok.extern.slf4j.Slf4j;

/**
 * 示例插件实现
 */
@Slf4j
public class PluginImpl extends BasePlugin {

    public PluginImpl() {
        super("示例设备集成插件", "1.0.0");
    }

    @Override
    public void init() {
        log.info("初始化插件：{}", getName());
    }

    @Override
    public void start() {
        log.info("启动插件：{}", getName());
    }

    @Override
    public void stop() {
        log.info("停止插件：{}", getName());
    }

    @Override
    public void destroy() {
        log.info("销毁插件：{}", getName());
    }

    @Override
    public Object execute(String methodName, Object... args) throws Exception {
        if ("ping".equals(methodName)) {
            return "pong";
        }
        throw new UnsupportedOperationException("不支持的方法：" + methodName);
    }

    @Override
    public void startInstance(String integrationId, Object config) throws Exception {
        log.info("启动集成实例：integrationId={}, config={}", integrationId, config);
        log.info("当前配置：{}", getCurrentConfig());
    }

    @Override
    public void stopInstance(String integrationId) throws Exception {
        log.info("停止集成实例：{}", integrationId);
    }

    @Override
    public Object handleDeviceProperty(String sourceData) {
        return sourceData;
    }

    @Override
    public Object handleDeviceStatus(String sourceData) {
        return sourceData;
    }

    @Override
    public Object handleTimeSeriesData(String sourceData) {
        return sourceData;
    }
}
