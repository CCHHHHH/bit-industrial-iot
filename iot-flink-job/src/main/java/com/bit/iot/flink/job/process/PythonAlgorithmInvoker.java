package com.bit.iot.flink.job.process;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Python 脚本算法调用器
 * <p>
 * 通过子进程调用系统 Python3 解释器执行用户上传的 Python 脚本。
 * 数据和参数通过环境变量 RULE_DATA_POINTS / RULE_PARAMS 以 JSON 传入。
 * 脚本将结果 JSON 打印到 stdout。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class PythonAlgorithmInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(PythonAlgorithmInvoker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TIMEOUT_SECONDS = 60;

    /**
     * 执行 Python 算法
     *
     * @param scriptPath Python 脚本路径
     * @param dataPoints 时序数据
     * @param params     自定义参数
     * @return 执行结果
     */
    public static AlgorithmResult execute(String scriptPath,
                                          List<DataPoint> dataPoints,
                                          Map<String, String> params) {
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            return AlgorithmResult.failure("Python 脚本不存在: " + scriptPath);
        }

        try {
            String dataJson = MAPPER.writeValueAsString(dataPoints);
            String paramsJson = MAPPER.writeValueAsString(params);

            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);
            pb.environment().put("RULE_DATA_POINTS", dataJson);
            pb.environment().put("RULE_PARAMS", paramsJson);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line);
                }
            }

            StringBuilder stderr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return AlgorithmResult.failure("Python 执行超时 (>" + TIMEOUT_SECONDS + "s)");
            }

            if (process.exitValue() != 0) {
                return AlgorithmResult.failure("Python 异常退出 (code=" + process.exitValue() + "): " + stderr);
            }

            String output = stdout.toString().trim();
            if (output.isEmpty()) {
                return AlgorithmResult.failure("Python 脚本无输出");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = MAPPER.readValue(output, Map.class);
            boolean success = Boolean.TRUE.equals(resultMap.get("success"));
            if (!success) {
                return AlgorithmResult.failure(String.valueOf(resultMap.get("errorMsg")));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) resultMap.getOrDefault("data", new HashMap<>());
            return AlgorithmResult.success(data);

        } catch (Exception e) {
            LOG.error("Python 执行异常: {}", scriptPath, e);
            return AlgorithmResult.failure("Python 执行异常: " + e.getMessage());
        }
    }
}
