package com.bit.iot.integration.plugin;

/**
 * 插件接口定义
 * 所有插件必须实现此接口
 *
 * @author chenhao
 * @since 2026-03-20
 */
public interface IPlugin {
    
    /**
     * 获取插件名称
     * @return 插件名称
     */
    String getName();
    
    /**
     * 获取插件版本
     * @return 插件版本
     */
    String getVersion();
    
    /**
     * 插件初始化方法
     */
    void init();
    
    /**
     * 插件启动方法
     */
    void start();
    
    /**
     * 插件停止方法
     */
    void stop();
    
    /**
     * 插件销毁方法
     */
    void destroy();
    
    /**
     * 执行插件方法
     * @param methodName 方法名
     * @param args 参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    Object execute(String methodName, Object... args) throws Exception;
    
    /**
     * 启动集成实例（可选实现）
     * @param integrationId 集成配置 ID
     * @param config 集成配置信息
     * @throws Exception 执行异常
     */
    default void startInstance(String integrationId, Object config) throws Exception {
        // 默认不实现，由插件选择性地重写
    }
    
    /**
     * 停止集成实例（可选实现）
     * @param integrationId 集成配置 ID
     * @throws Exception 执行异常
     */
    default void stopInstance(String integrationId) throws Exception {
        // 默认不实现，由插件选择性地重写
    }

    /**
     * 获取设备属性（对应 MappingTypeEnum.DEVICE_PROPERTY）
     * @param sourceData 原始数据（JSON 格式）
     * @return 设备属性数据
     * @throws Exception 执行异常
     */
    default Object handleDeviceProperty(String sourceData) throws Exception {
        return null;
    }

    /**
     * 获取设备状态（对应 MappingTypeEnum.DEVICE_STATUS）
     * @param sourceData 原始数据（JSON 格式）
     * @return 设备状态数据
     * @throws Exception 执行异常
     */
    default Object handleDeviceStatus(String sourceData) throws Exception {
        return null;
    }

    /**
     * 获取时序数据（对应 MappingTypeEnum.TIME_SERIES_DATA）
     * @param sourceData 原始数据（JSON 格式）
     * @return 时序数据
     * @throws Exception 执行异常
     */
    default Object handleTimeSeriesData(String sourceData) throws Exception {
        return null;
    }
}
