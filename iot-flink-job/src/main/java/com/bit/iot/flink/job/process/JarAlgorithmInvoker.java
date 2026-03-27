package com.bit.iot.flink.job.process;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.common.flink.IRuleAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

/**
 * JAR 算法调用器
 * <p>
 * 在 Flink TaskManager 中通过 URLClassLoader 动态加载用户上传的 JAR 包，
 * 并调用其中实现 IRuleAlgorithm 接口的算法类。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class JarAlgorithmInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(JarAlgorithmInvoker.class);

    private IRuleAlgorithm instance;
    private URLClassLoader classLoader;

    /**
     * 加载算法
     *
     * @param jarPath        JAR 文件路径（需 TaskManager 可访问，如 NFS / HDFS）
     * @param algorithmClass 入口类全限定名
     */
    public void load(String jarPath, String algorithmClass) throws Exception {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IllegalStateException("算法 JAR 文件不存在: " + jarPath);
        }

        classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                Thread.currentThread().getContextClassLoader()
        );

        Class<?> clazz = classLoader.loadClass(algorithmClass);
        if (!IRuleAlgorithm.class.isAssignableFrom(clazz)) {
            classLoader.close();
            throw new IllegalArgumentException("算法类必须实现 IRuleAlgorithm 接口: " + algorithmClass);
        }

        instance = (IRuleAlgorithm) clazz.getDeclaredConstructor().newInstance();
        LOG.info("JAR 算法加载成功: {} -> {}", jarPath, algorithmClass);
    }

    /**
     * 执行算法
     */
    public AlgorithmResult execute(List<DataPoint> dataPoints, Map<String, String> params) {
        if (instance == null) {
            return AlgorithmResult.failure("算法未加载");
        }
        return instance.execute(dataPoints, params);
    }

    /**
     * 释放资源
     */
    public void close() {
        instance = null;
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (Exception ignored) {
            }
        }
    }
}
