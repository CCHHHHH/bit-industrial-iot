package com.bit.iot.rule.engine;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Python 脚本算法执行器
 * <p>
 * 通过子进程调用系统 Python3 解释器执行 Python 脚本。
 * 数据点和参数以 JSON 格式通过环境变量传入，脚本执行结果从 stdout 读取。
 * </p>
 *
 * <p>Python 脚本约定：</p>
 * <ul>
 *   <li>从环境变量 {@code RULE_DATA_POINTS} 读取数据点 JSON</li>
 *   <li>从环境变量 {@code RULE_PARAMS} 读取参数 JSON</li>
 *   <li>将结果以 JSON 字符串打印到 stdout（最后一行）</li>
 *   <li>执行成功时包含 {@code "success": true}，失败时包含 {@code "success": false, "errorMsg": "..."}</li>
 * </ul>
 *
 * <p>Python 脚本模板：</p>
 * <pre>
 * import os, json
 * data_points = json.loads(os.environ.get('RULE_DATA_POINTS', '[]'))
 * params = json.loads(os.environ.get('RULE_PARAMS', '{}'))
 * # 自定义处理逻辑
 * avg = sum(p['value'] for p in data_points) / len(data_points) if data_points else 0
 * print(json.dumps({"success": True, "data": {"avg": avg}}))
 * </pre>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Slf4j
public class PythonAlgorithmExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Python 子进程执行超时时间（秒） */
    private static final int TIMEOUT_SECONDS = 60;

    /**
     * 执行 Python 脚本
     *
     * @param scriptPath Python 脚本文件路径
     * @param dataPoints 时序数据点
     * @param params     自定义参数
     * @return 算法执行结果
     */
    public static AlgorithmResult execute(String scriptPath,
                                          List<DataPoint> dataPoints,
                                          Map<String, String> params) {
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            return AlgorithmResult.failure("Python 脚本文件不存在：" + scriptPath);
        }

        try {
            // 序列化数据
            String dataPointsJson = MAPPER.writeValueAsString(dataPoints);
            String paramsJson = MAPPER.writeValueAsString(params);

            // 构建子进程
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);
            pb.environment().put("RULE_DATA_POINTS", dataPointsJson);
            pb.environment().put("RULE_PARAMS", paramsJson);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // 读取 stdout
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line);
                }
            }

            // 读取 stderr
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
                return AlgorithmResult.failure("Python 脚本执行超时（> " + TIMEOUT_SECONDS + "s）");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return AlgorithmResult.failure("Python 脚本异常退出（exitCode=" + exitCode + "）：" + stderr);
            }

            // 解析最后一行 JSON 输出
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
            log.error("执行 Python 脚本失败：{}", scriptPath, e);
            return AlgorithmResult.failure("执行 Python 脚本异常：" + e.getMessage());
        }
    }
}
