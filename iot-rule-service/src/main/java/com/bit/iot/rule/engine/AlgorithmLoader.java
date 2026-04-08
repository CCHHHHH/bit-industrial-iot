package com.bit.iot.rule.engine;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.common.flink.algorithm.JarAlgorithmLoader;
import com.bit.iot.common.flink.algorithm.PythonAlgorithmExecutor;
import com.bit.iot.rule.model.entity.RuleAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 算法加载器
 * <p>
 * 负责动态加载 JAR 包算法，以及通过 {@link PythonAlgorithmExecutor} 执行 Python 脚本算法。
 * JAR 算法按 algorithmId 缓存 {@link JarAlgorithmLoader}，支持热替换（{@link #unload}）。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Slf4j
@Component
public class AlgorithmLoader {

    /** JAR 算法缓存：algorithmId -> JarAlgorithmLoader（含 ClassLoader 及算法实例） */
    private final Map<String, JarAlgorithmLoader> loaderCache = new ConcurrentHashMap<>();

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
     * 执行 JAR 算法，按 algorithmId 缓存 {@link JarAlgorithmLoader}
     */
    private AlgorithmResult executeJar(RuleAlgorithm algorithm,
                                       List<DataPoint> dataPoints,
                                       Map<String, String> params) throws Exception {
        String algorithmId = algorithm.getId();
        JarAlgorithmLoader loader = loaderCache.get(algorithmId);

        if (loader == null) {
            log.info("加载 JAR 算法：{} -> {}", algorithm.getAlgorithmName(), algorithm.getAlgorithmPath());
            loader = new JarAlgorithmLoader();
            loader.load(algorithm.getAlgorithmPath(), algorithm.getAlgorithmClass());
            loaderCache.put(algorithmId, loader);
        }

        return loader.execute(dataPoints, params);
    }

    /**
     * 卸载算法（更新文件后调用以触发热替换）
     *
     * @param algorithmId 算法 ID
     */
    public void unload(String algorithmId) {
        JarAlgorithmLoader loader = loaderCache.remove(algorithmId);
        if (loader != null) {
            loader.close();
        }
        log.info("算法已卸载：{}", algorithmId);
    }

    /**
     * 卸载全部算法
     */
    public void unloadAll() {
        loaderCache.forEach((id, loader) -> loader.close());
        loaderCache.clear();
        log.info("所有算法已卸载");
    }
}
