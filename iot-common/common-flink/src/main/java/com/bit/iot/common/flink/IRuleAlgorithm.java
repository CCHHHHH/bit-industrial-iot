package com.bit.iot.common.flink;

import java.util.List;
import java.util.Map;

/**
 * 规则算法接口（公共契约）
 * <p>
 * 所有通过 JAR 包上传的自定义算法必须实现此接口。
 * 入口类需提供无参构造函数，全限定类名填写在 rule_algorithm.algorithm_class 字段。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * public class VibrationAnalyzer implements IRuleAlgorithm {
 *     public AlgorithmResult execute(List&lt;DataPoint&gt; dataPoints, Map&lt;String, String&gt; params) {
 *         double rms = Math.sqrt(dataPoints.stream()
 *             .mapToDouble(p -&gt; p.getValue() * p.getValue()).average().orElse(0));
 *         double threshold = Double.parseDouble(params.getOrDefault("threshold", "5.0"));
 *         boolean alert = rms &gt; threshold;
 *         return AlgorithmResult.success(Map.of("rms", rms, "alert", alert));
 *     }
 * }
 * </pre>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public interface IRuleAlgorithm {

    /**
     * 执行算法
     *
     * @param dataPoints 从 TDEngine 读取的时序数据点列表
     * @param params     规则配置的自定义参数（key-value）
     * @return 算法执行结果
     */
    AlgorithmResult execute(List<DataPoint> dataPoints, Map<String, String> params);
}
