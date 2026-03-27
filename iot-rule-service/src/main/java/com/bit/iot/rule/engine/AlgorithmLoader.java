package com.bit.iot.rule.engine;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.common.flink.IRuleAlgorithm;
import com.bit.iot.rule.model.entity.RuleAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 算法加载器
 * <p>
 * 负责动态加载 JAR 包算法，并通过 PythonAlgorithmExecutor 执行 Python 脚本算法。
 * 每个 JAR 算法使用独立的 ClassLoader 进行类隔离，支持热替换。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Slf4j
@Component
public class AlgorithmLoader {

    /** 已加载的 JAR 算法缓存：algorithmId -> IRuleAlgorithm 实例 */
    private final Map<String, IRuleAlgorithm> algorithmCache = new ConcurrentHashMap<>();

    /** 对应的 ClassLoader 缓存（用于卸载时关闭） */
    private final Map<String, URLClassLoader> classLoaderCache = new ConcurrentHashMap<>();

    /**
     * 加载并执行算法
     *
     * @param algorithm  算法元数据
     * @param dataPoints 时序数据点
     * @param params     自定义参数
     * @return 执行结果
     */
    public AlgorithmResult execute(RuleAlgorithm algorithm,
                                   List<DataPoint> dataPoints,
                                   Map<String, String> params) throws Exception {
        String type = algorithm.getAlgorithmType();
        if ("jar".equalsIgnoreCase(type)) {
            return executeJar(algorithm, dataPoints, params);
        } else if ("python".equalsIgnoreCase(type)) {
            return PythonAlgorithmExecutor.execute(algorithm.getAlgorithmPath(), dataPoints, params);
        } else {
            throw new IllegalArgumentException("不支持的算法类型：" + type);
        }
    }

    /**
     * 执行 JAR 算法
     */
    private AlgorithmResult executeJar(RuleAlgorithm algorithm,
                                        List<DataPoint> dataPoints,
                                        Map<String, String> params) throws Exception {
        String algorithmId = algorithm.getId();
        IRuleAlgorithm instance = algorithmCache.get(algorithmId);

        if (instance == null) {
            instance = loadJarAlgorithm(algorithm);
        }

        return instance.execute(dataPoints, params);
    }

    /**
     * 动态加载 JAR 包中的算法类
     */
    private IRuleAlgorithm loadJarAlgorithm(RuleAlgorithm algorithm) throws Exception {
        String algorithmId = algorithm.getId();
        log.info("加载 JAR 算法：{} -> {}", algorithm.getAlgorithmName(), algorithm.getAlgorithmPath());

        File jarFile = new File(algorithm.getAlgorithmPath());
        if (!jarFile.exists()) {
            throw new IllegalStateException("算法 JAR 文件不存在：" + algorithm.getAlgorithmPath());
        }

        // 关闭旧的 ClassLoader（如果存在）
        URLClassLoader old = classLoaderCache.get(algorithmId);
        if (old != null) {
            try { old.close(); } catch (Exception ignored) {}
        }

        // 创建独立 ClassLoader，父 ClassLoader 为当前线程上下文
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                Thread.currentThread().getContextClassLoader()
        );

        Class<?> clazz = classLoader.loadClass(algorithm.getAlgorithmClass());
        if (!IRuleAlgorithm.class.isAssignableFrom(clazz)) {
            classLoader.close();
            throw new IllegalArgumentException("算法类必须实现 IRuleAlgorithm 接口：" + algorithm.getAlgorithmClass());
        }

        IRuleAlgorithm instance = (IRuleAlgorithm) clazz.getDeclaredConstructor().newInstance();
        algorithmCache.put(algorithmId, instance);
        classLoaderCache.put(algorithmId, classLoader);

        log.info("JAR 算法加载成功：{}", algorithm.getAlgorithmName());
        return instance;
    }

    /**
     * 卸载算法（更新文件后调用以触发热替换）
     *
     * @param algorithmId 算法 ID
     */
    public void unload(String algorithmId) {
        algorithmCache.remove(algorithmId);
        URLClassLoader loader = classLoaderCache.remove(algorithmId);
        if (loader != null) {
            try { loader.close(); } catch (Exception ignored) {}
        }
        log.info("算法已卸载：{}", algorithmId);
    }

    /**
     * 卸载全部算法
     */
    public void unloadAll() {
        algorithmCache.clear();
        classLoaderCache.forEach((id, loader) -> {
            try { loader.close(); } catch (Exception ignored) {}
        });
        classLoaderCache.clear();
        log.info("所有算法已卸载");
    }
}
