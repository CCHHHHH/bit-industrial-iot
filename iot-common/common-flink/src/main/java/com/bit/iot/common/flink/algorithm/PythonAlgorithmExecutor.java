package com.bit.iot.common.flink.algorithm;

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
 * Python 脚本算法执行器（可复用核心）
 * <p>
 * 通过子进程调用系统 {@code python3} 解释器执行用户上传的 Python 脚本。
 * 数据和参数通过环境变量以 JSON 传入，脚本将结果 JSON 打印到 stdout。
 * </p>
 *
 * <p>Python 脚本约定：</p>
 * <ul>
 *   <li>从环境变量 {@code RULE_DATA_POINTS} 读取数据点 JSON</li>
 *   <li>从环境变量 {@code RULE_PARAMS} 读取参数 JSON</li>
 *   <li>将结果以 JSON 字符串打印到 stdout</li>
 *   <li>成功：{@code {"success": true, "data": {...}}}</li>
 *   <li>失败：{@code {"success": false, "errorMsg": "..."}}</li>
 * </ul>
 *
 * <p>Python 脚本模板：</p>
 * <pre>
 * import os, json
 * data_points = json.loads(os.environ.get('RULE_DATA_POINTS', '[]'))
 * params = json.loads(os.environ.get('RULE_PARAMS', '{}'))
 * avg = sum(p['value'] for p in data_points) / len(data_points) if data_points else 0
 * print(json.dumps({"success": True, "data": {"avg": avg}}))
 * </pre>
 *
 * @author chenhao
 * @since 2026-03-31
 */
public class PythonAlgorithmExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PythonAlgorithmExecutor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TIMEOUT_SECONDS = 60;
    private static final int MAX_OUTPUT_LENGTH = 16 * 1024;

    private PythonAlgorithmExecutor() {}

    /**
     * 执行 Python 脚本算法
     *
     * @param scriptPath Python 脚本文件路径
     * @param dataPoints 时序数据点
     * @param params     规则自定义参数
     * @return 执行结果
     */
    public static AlgorithmResult execute(String scriptPath,
                                          List<DataPoint> dataPoints,
                                          Map<String, String> params) {
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            return AlgorithmResult.failure("Python 脚本文件不存在: " + scriptPath);
        }
        if (!scriptFile.getName().endsWith(".py")) {
            return AlgorithmResult.failure("Python 算法文件必须是 .py");
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
                    if (stdout.length() > MAX_OUTPUT_LENGTH) {
                        process.destroyForcibly();
                        return AlgorithmResult.failure("Python stdout 超出限制");
                    }
                }
            }

            StringBuilder stderr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                    if (stderr.length() > MAX_OUTPUT_LENGTH) {
                        process.destroyForcibly();
                        return AlgorithmResult.failure("Python stderr 超出限制");
                    }
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return AlgorithmResult.failure("Python 执行超时 (>" + TIMEOUT_SECONDS + "s)");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return AlgorithmResult.failure("Python 异常退出 (code=" + exitCode + "): " + stderr);
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
