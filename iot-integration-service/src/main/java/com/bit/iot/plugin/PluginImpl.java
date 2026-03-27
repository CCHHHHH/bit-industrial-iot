package com.bit.iot.plugin;

import com.bit.iot.integration.plugin.BasePlugin;
import lombok.extern.slf4j.Slf4j;

/**
 * 示例插件实现
 * 开发者可以参考这个示例来开发自己的插件
 *
 * @author chenhao
 * @since 2026-03-20
 */
@Slf4j
public class PluginImpl extends BasePlugin {
    
    public PluginImpl() {
        super("示例插件", "1.0.0");
    }
    
    @Override
    public void init() {
        log.info("初始化插件：{}", getName());
        // 在这里进行初始化操作，比如加载配置文件、初始化连接池等
    }
    
    @Override
    public void start() {
        log.info("启动插件：{}", getName());
        // 在这里启动插件的功能
    }
    
    @Override
    public void stop() {
        log.info("停止插件：{}", getName());
        // 在这里停止插件的功能
    }
    
    @Override
    public void destroy() {
        log.info("销毁插件：{}", getName());
        // 在这里清理资源
    }
    
    @Override
    public Object execute(String methodName, Object... args) throws Exception {
        log.info("执行方法：{}({})", methodName, args);
        
        // 根据方法名调用不同的方法
        switch (methodName) {
            case "sayHello":
                return sayHello((String) args[0]);
            
            case "calculate":
                return calculate((Integer) args[0], (Integer) args[1]);
            
            case "processData":
                return processData((String) args[0]);
            
            default:
                throw new UnsupportedOperationException("不支持的方法：" + methodName);
        }
    }
    
    /**
     * 启动集成实例（实现 IPlugin 接口）
     * @param integrationId 集成配置 ID
     * @param config 配置对象
     */
    @Override
    public void startInstance(String integrationId, Object config) throws Exception {
        log.info("启动集成实例：integrationId={}, config={}", integrationId, config);
        
        // 这里可以实现具体的启动逻辑
        // 例如：初始化数据源、启动定时器、连接外部系统等
        
        // 模拟启动操作
        Thread.sleep(100); // 模拟初始化时间
        
        log.info("集成实例已启动：{}", integrationId);
    }
    
    /**
     * 停止集成实例（实现 IPlugin 接口）
     * @param integrationId 集成配置 ID
     */
    @Override
    public void stopInstance(String integrationId) throws Exception {
        log.info("停止集成实例：{}", integrationId);
        
        // 这里可以实现具体的停止逻辑
        // 例如：关闭数据源、停止定时器、断开外部连接等
        
        // 模拟停止操作
        Thread.sleep(50); // 模拟清理时间
        
        log.info("集成实例已停止：{}", integrationId);
    }
    
    /**
     * 示例方法：打招呼
     * @param name 名称
     * @return 问候语
     */
    public String sayHello(String name) {
        log.info("收到问候：{}", name);
        return "你好，" + name + "！来自插件：" + getName();
    }
    
    /**
     * 示例方法：计算加法
     * @param a 数字 1
     * @param b 数字 2
     * @return 和
     */
    public Integer calculate(Integer a, Integer b) {
        log.info("计算：{} + {}", a, b);
        return a + b;
    }
    
    /**
     * 示例方法：处理数据
     * @param data 数据
     * @return 处理结果
     */
    public String processData(String data) {
        log.info("处理数据：{}", data);
        // 这里可以添加实际的数据处理逻辑
        return "已处理：" + data;
    }
}
