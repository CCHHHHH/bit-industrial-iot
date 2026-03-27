package com.bit.iot.integration.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 插件类加载器
 * 支持热加载和隔离不同版本的插件
 *
 * @author chenhao
 * @since 2026-03-20
 */
public class PluginClassLoader extends URLClassLoader {
    
    private final String pluginId;
    private final String pluginName;
    
    public PluginClassLoader(String pluginId, String pluginName, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginId = pluginId;
        this.pluginName = pluginName;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public String getPluginName() {
        return pluginName;
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 首先检查是否已经加载过
            Class<?> loadedClass = findLoadedClass(name);
            
            if (loadedClass == null) {
                // 判断是否是插件类（需要隔离的类）
                if (isPluginClass(name)) {
                    try {
                        // 优先从插件 jar 包中加载类
                        loadedClass = findClass(name);
                    } catch (ClassNotFoundException e) {
                        // 如果插件中没有，则委托给父类加载器
                        loadedClass = super.loadClass(name, resolve);
                    }
                } else {
                    // 共享类，直接委托给父类加载器（使用主应用的依赖）
                    try {
                        loadedClass = super.loadClass(name, resolve);
                    } catch (ClassNotFoundException e) {
                        // 父类没有，再尝试从插件中查找
                        loadedClass = findClass(name);
                    }
                }
            }
            
            if (resolve) {
                resolveClass(loadedClass);
            }
            
            return loadedClass;
        }
    }
    
    /**
     * 判断是否是插件类（需要隔离的类）
     * @param className 类名
     * @return true-需要隔离，false-可以共享
     */
    private boolean isPluginClass(String className) {
        // 插件自己的类需要隔离
        if (className.startsWith("com.bit.iot.plugin")) {
            return true;
        }
        
        // 这里可以配置哪些包需要隔离，哪些可以共享
        // 例如：插件特有的第三方库需要隔离
        if (className.startsWith("com.myplugin.util")) {
            return true;
        }
        
        // 其他类可以共享主应用的依赖
        return false;
    }
    
    /**
     * 关闭类加载器，释放资源
     */
    public void close() {
        try {
            for (URL url : getURLs()) {
                // 可以添加资源清理逻辑
            }
            super.close();
        } catch (Exception e) {
            // 忽略关闭异常
        }
    }
}
