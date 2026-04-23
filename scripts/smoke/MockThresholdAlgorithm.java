package smoke;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.common.flink.IRuleAlgorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockThresholdAlgorithm implements IRuleAlgorithm {

    @Override
    public AlgorithmResult execute(List<DataPoint> dataPoints, Map<String, String> params) {
        double maxValue = dataPoints.stream()
                .mapToDouble(DataPoint::getValue)
                .max()
                .orElse(0.0D);
        double threshold = Double.parseDouble(params.getOrDefault("threshold", "0"));
        boolean alert = maxValue > threshold;

        Map<String, Object> result = new HashMap<>();
        result.put("alert", alert);
        result.put("alertLevel", params.getOrDefault("alertLevel", "warning"));
        result.put("alertMessage", params.getOrDefault("alertMessage", "模拟阈值告警"));
        result.put("metricName", "temperature");
        result.put("metricValue", String.valueOf(maxValue));
        result.put("maxValue", maxValue);
        return AlgorithmResult.success(result);
    }
}
