package com.bit.iot.common.flink.algorithm;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.common.flink.IRuleAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

/**
 * JAR 算法加载器（可复用核心）
 * <p>
 * 通过 URLClassLoader 动态加载用户上传的 JAR 包，并调用其中实现
 * {@link IRuleAlgorithm} 接口的算法类。
 * </p>
 * <p>
 * 每个实例持有一个 JAR 的 ClassLoader，生命周期由调用方管理（调用
 * {@link #close()} 释放资源）。
 * </p>
 * <ul>
 *   <li>在 Flink TaskManager 中由 {@code AlgorithmWindowFunction} 在 open/close 管理。</li>
 *   <li>在 iot-rule-service 本地模式中由 {@code AlgorithmLoader} 按 algorithmId 缓存。</li>
 * </ul>
 *
 * @author chenhao
 * @since 2026-03-31
 */
public class JarAlgorithmLoader implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(JarAlgorithmLoader.class);

    private IRuleAlgorithm instance;
    private URLClassLoader classLoader;

    /**
     * 从指定 JAR 文件加载算法类
     *
     * @param jarPath        JAR 文件路径
     * @param algorithmClass 入口类全限定名，必须实现 {@link IRuleAlgorithm}
     * @throws Exception JAR 不存在、类不存在或接口不匹配时抛出
     */
    public void load(String jarPath, String algorithmClass) throws Exception {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IllegalStateException("算法 JAR 文件不存在: " + jarPath);
        }
        if (!jarFile.getCanonicalPath().endsWith(".jar")) {
            throw new IllegalArgumentException("算法文件必须是 .jar: " + jarPath);
        }
        if (algorithmClass == null || algorithmClass.isBlank()) {
            throw new IllegalArgumentException("JAR 算法必须指定 algorithmClass");
        }

        classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                Thread.currentThread().getContextClassLoader()
        );

        Class<?> clazz = classLoader.loadClass(algorithmClass);
        if (!IRuleAlgorithm.class.isAssignableFrom(clazz)) {
            classLoader.close();
            classLoader = null;
            throw new IllegalArgumentException("算法类必须实现 IRuleAlgorithm 接口: " + algorithmClass);
        }

        instance = (IRuleAlgorithm) clazz.getDeclaredConstructor().newInstance();
        LOG.info("JAR 算法加载成功: {} -> {}", jarPath, algorithmClass);
    }

    /**
     * 执行算法
     *
     * @param dataPoints 时序数据点
     * @param params     规则自定义参数
     * @return 执行结果
     */
    public AlgorithmResult execute(List<DataPoint> dataPoints, Map<String, String> params) {
        if (instance == null) {
            return AlgorithmResult.failure("算法未加载，请先调用 load()");
        }
        return instance.execute(dataPoints, params);
    }

    /**
     * 释放 ClassLoader 及算法实例（热替换时使用）
     */
    @Override
    public void close() {
        instance = null;
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (Exception ignored) {
            }
            classLoader = null;
        }
    }
}
