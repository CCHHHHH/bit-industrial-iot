package com.bit.iot.rule.flink;

import com.bit.iot.common.flink.RuleJobConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flink 集群交互管理器
 * <p>
 * 通过 Flink REST API 提交、取消、监控 Flink Job。
 * 替代原有的 RuleEngineManager（线程池调度）。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Component
public class FlinkJobManager {

    private static final Logger LOG = LoggerFactory.getLogger(FlinkJobManager.class);

    @Value("${flink.rest.url:http://localhost:8081}")
    private String flinkRestUrl;

    @Value("${flink.job.jar-path:}")
    private String flinkJobJarPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 已提交的 Job 映射：ruleId → Flink jobId */
    private final Map<String, String> ruleJobMapping = new ConcurrentHashMap<>();

    /** 上传后的 Flink JAR ID（upload 一次即可复用） */
    private volatile String uploadedJarId;

    // ================================================================
    // 提交 Job
    // ================================================================

    /**
     * 提交规则对应的 Flink Job
     *
     * @param config 规则 Job 配置
     * @return Flink Job ID
     */
    public String submitJob(RuleJobConfig config) throws Exception {
        // 1. 确保 JAR 已上传
        ensureJarUploaded();

        // 2. 将配置序列化为 Base64
        String configJson = objectMapper.writeValueAsString(config);
        String base64Config = Base64.getEncoder().encodeToString(
                configJson.getBytes(StandardCharsets.UTF_8));

        // 3. 调用 Flink REST API 提交 Job
        String url = flinkRestUrl + "/jars/" + uploadedJarId + "/run";

        Map<String, Object> body = new HashMap<>();
        body.put("entryClass", "com.bit.iot.flink.job.RuleJobEntrypoint");
        body.put("programArgs", "--ruleConfig " + base64Config);
        body.put("parallelism", config.getParallelism());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        if (resp.getBody() == null || !resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Flink Job 提交失败: " + resp.getStatusCode());
        }

        String jobId = (String) resp.getBody().get("jobid");
        ruleJobMapping.put(config.getRuleId(), jobId);

        LOG.info("规则 {} 已提交 Flink Job: {}", config.getRuleId(), jobId);
        return jobId;
    }

    // ================================================================
    // 取消 Job
    // ================================================================

    /**
     * 取消规则对应的 Flink Job
     *
     * @param ruleId        规则 ID
     * @param withSavepoint 是否触发 Savepoint 后取消（用于升级场景）
     */
    public void cancelJob(String ruleId, boolean withSavepoint) throws Exception {
        String jobId = ruleJobMapping.get(ruleId);
        if (jobId == null) {
            LOG.warn("未找到规则 {} 对应的 Flink Job，可能已取消", ruleId);
            return;
        }

        if (withSavepoint) {
            String url = flinkRestUrl + "/jobs/" + jobId + "/savepoints";
            Map<String, Object> body = new HashMap<>();
            body.put("cancel-job", true);
            body.put("target-directory", "hdfs:///flink/savepoints");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        } else {
            String url = flinkRestUrl + "/jobs/" + jobId + "/yarn-cancel";
            try {
                restTemplate.getForObject(url, Map.class);
            } catch (Exception e) {
                // 尝试通过 PATCH 取消
                try {
                    url = flinkRestUrl + "/jobs/" + jobId;
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    Map<String, String> body = Map.of("state", "canceled");
                    restTemplate.patchForObject(url, new HttpEntity<>(body, headers), Map.class);
                } catch (Exception ex) {
                    LOG.warn("取消 Flink Job 失败，尝试直接 cancel: {}", ex.getMessage());
                }
            }
        }

        ruleJobMapping.remove(ruleId);
        LOG.info("规则 {} 的 Flink Job {} 已取消", ruleId, jobId);
    }

    // ================================================================
    // 查询 Job 状态
    // ================================================================

    /**
     * 查询规则对应的 Flink Job 状态
     */
    public FlinkJobStatus getJobStatus(String ruleId) {
        String jobId = ruleJobMapping.get(ruleId);
        if (jobId == null) return FlinkJobStatus.NOT_FOUND;

        try {
            String url = flinkRestUrl + "/jobs/" + jobId;
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null) {
                String state = (String) resp.get("state");
                return FlinkJobStatus.safeValueOf(state);
            }
        } catch (Exception e) {
            LOG.warn("查询 Flink Job 状态失败: ruleId={}, error={}", ruleId, e.getMessage());
        }
        return FlinkJobStatus.UNKNOWN;
    }

    /**
     * 获取规则对应的 Flink Job ID
     */
    public String getFlinkJobId(String ruleId) {
        return ruleJobMapping.get(ruleId);
    }

    /**
     * 手动注册已存在的 Flink Job 映射（服务重启后恢复用）
     */
    public void registerJobMapping(String ruleId, String flinkJobId) {
        if (ruleId != null && flinkJobId != null) {
            ruleJobMapping.put(ruleId, flinkJobId);
        }
    }

    // ================================================================
    // JAR 上传
    // ================================================================

    private synchronized void ensureJarUploaded() throws Exception {
        if (uploadedJarId != null) return;

        if (flinkJobJarPath == null || flinkJobJarPath.isEmpty()) {
            throw new RuntimeException("未配置 flink.job.jar-path，无法上传 Flink Job JAR");
        }

        File jarFile = new File(flinkJobJarPath);
        if (!jarFile.exists()) {
            throw new RuntimeException("Flink Job JAR 不存在: " + flinkJobJarPath);
        }

        String url = flinkRestUrl + "/jars/upload";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("jarfile", new FileSystemResource(jarFile));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        if (resp.getBody() == null) {
            throw new RuntimeException("Flink JAR 上传失败：无响应");
        }

        String filename = (String) resp.getBody().get("filename");
        if (filename != null) {
            uploadedJarId = filename.substring(filename.lastIndexOf('/') + 1);
        }

        LOG.info("Flink Job JAR 上传成功: {}", uploadedJarId);
    }
}
