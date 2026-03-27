package com.bit.iot.flink.job.process;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.flink.job.model.AlgorithmOutputEvent;
import com.bit.iot.flink.job.model.DeviceDataEvent;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 算法窗口处理函数
 * <p>
 * 在每个时间窗口触发时，将窗口内的 DeviceDataEvent 转换为 DataPoint 列表，
 * 调用用户上传的算法（JAR / Python），输出 AlgorithmOutputEvent。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class AlgorithmWindowFunction
        extends ProcessWindowFunction<DeviceDataEvent, AlgorithmOutputEvent, String, TimeWindow> {

    private static final Logger LOG = LoggerFactory.getLogger(AlgorithmWindowFunction.class);

    private final String ruleId;
    private final String algorithmType;
    private final String algorithmPath;
    private final String algorithmClass;
    private final Map<String, String> ruleParams;

    /** JAR 算法调用器（懒加载） */
    private transient JarAlgorithmInvoker jarInvoker;

    public AlgorithmWindowFunction(String ruleId,
                                   String algorithmType,
                                   String algorithmPath,
                                   String algorithmClass,
                                   Map<String, String> ruleParams) {
        this.ruleId = ruleId;
        this.algorithmType = algorithmType;
        this.algorithmPath = algorithmPath;
        this.algorithmClass = algorithmClass;
        this.ruleParams = ruleParams != null ? ruleParams : Collections.emptyMap();
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        if ("jar".equalsIgnoreCase(algorithmType)) {
            jarInvoker = new JarAlgorithmInvoker();
            jarInvoker.load(algorithmPath, algorithmClass);
        }
    }

    @Override
    public void close() throws Exception {
        if (jarInvoker != null) {
            jarInvoker.close();
        }
    }

    @Override
    public void process(String key,
                        ProcessWindowFunction<DeviceDataEvent, AlgorithmOutputEvent, String, TimeWindow>.Context context,
                        Iterable<DeviceDataEvent> elements,
                        Collector<AlgorithmOutputEvent> out) {

        // 1. 转换为 DataPoint 列表
        List<DataPoint> dataPoints = new ArrayList<>();
        for (DeviceDataEvent event : elements) {
            dataPoints.add(new DataPoint(
                    event.getDeviceId(),
                    event.getPointCode(),
                    new Date(event.getTimestamp()),
                    event.getValue(),
                    event.getQuality()
            ));
        }

        if (dataPoints.isEmpty()) return;

        // 2. 执行算法
        long startMs = System.currentTimeMillis();
        AlgorithmResult result;

        try {
            if ("jar".equalsIgnoreCase(algorithmType)) {
                result = jarInvoker.execute(dataPoints, ruleParams);
            } else if ("python".equalsIgnoreCase(algorithmType)) {
                result = PythonAlgorithmInvoker.execute(algorithmPath, dataPoints, ruleParams);
            } else {
                result = AlgorithmResult.failure("不支持的算法类型: " + algorithmType);
            }
        } catch (Exception e) {
            LOG.error("算法执行异常: key={}", key, e);
            result = AlgorithmResult.failure("算法异常: " + e.getMessage());
        }

        long durationMs = System.currentTimeMillis() - startMs;

        // 3. 构建输出事件
        AlgorithmOutputEvent output = new AlgorithmOutputEvent();
        output.setRuleId(ruleId);
        output.setKey(key);
        output.setWindowStart(context.window().getStart());
        output.setWindowEnd(context.window().getEnd());
        output.setSuccess(result.isSuccess());
        output.setResultData(result.getData());
        output.setErrorMsg(result.getErrorMsg());
        output.setDurationMs(durationMs);
        output.setProcessTime(System.currentTimeMillis());

        out.collect(output);

        LOG.debug("窗口计算完成: key={}, window=[{}-{}], success={}, duration={}ms",
                key, context.window().getStart(), context.window().getEnd(),
                result.isSuccess(), durationMs);
    }
}
