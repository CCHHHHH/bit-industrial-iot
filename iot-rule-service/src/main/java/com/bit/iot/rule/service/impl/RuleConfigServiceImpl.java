package com.bit.iot.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.rule.dao.RuleConfigMapper;
import com.bit.iot.rule.dao.RuleDataSourceMapper;
import com.bit.iot.rule.dao.RuleParamMapper;
import com.bit.iot.rule.engine.RuleEngineManager;
import com.bit.iot.rule.flink.FlinkJobManager;
import com.bit.iot.rule.flink.FlinkJobStatus;
import com.bit.iot.rule.model.dto.RuleConfigDetailDTO;
import com.bit.iot.rule.model.dto.RuleConfigListItemDTO;
import com.bit.iot.rule.model.entity.*;
import com.bit.iot.rule.model.enums.RuleStatusEnum;
import com.bit.iot.rule.model.vo.RuleConfigVO;
import com.bit.iot.rule.model.vo.RuleDataSourceVO;
import com.bit.iot.rule.model.vo.RuleParamVO;
import com.bit.iot.rule.model.enums.WindowUnitEnum;
import com.bit.iot.rule.service.IAlarmService;
import com.bit.iot.rule.service.IRuleAlgorithmService;
import com.bit.iot.rule.service.IRuleConfigService;
import com.bit.iot.rule.service.IRuleExecutionLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 规则配置 ServiceImpl
 * <p>
 * 支持两种引擎模式（通过 rule.engine.type 配置）：
 * <ul>
 *   <li><b>flink</b>：提交 Flink Job 到集群（生产模式）</li>
 *   <li><b>local</b>：本地线程池调度（开发调试）</li>
 * </ul>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Slf4j
@Service
public class RuleConfigServiceImpl extends ServiceImpl<RuleConfigMapper, RuleConfig>
        implements IRuleConfigService {

    private static final String MOCK_SOURCE_ENABLED_KEY = "_mock_source_enabled";
    private static final String MOCK_SOURCE_EVENTS_JSON_KEY = "_mock_source_events_json";

    @Autowired
    private RuleDataSourceMapper dataSourceMapper;

    @Autowired
    private RuleParamMapper paramMapper;

    @Autowired
    private IRuleAlgorithmService algorithmService;

    @Autowired
    private IRuleExecutionLogService executionLogService;

    @Autowired
    private IAlarmService alarmService;

    @Autowired
    private RuleEngineManager ruleEngineManager;

    @Autowired
    private FlinkJobManager flinkJobManager;

    @Value("${rule.engine.type:flink}")
    private String engineType;

    @Value("${rule.algorithm.shared-path:./algorithms}")
    private String sharedAlgorithmPath;

    @Value("${tdengine.result-stable:rule_result_stable}")
    private String tdengineResultStable;

    @Value("${tdengine.enabled:true}")
    private boolean tdengineEnabled;

    @Value("${tdengine.jdbc-url:}")
    private String tdengineJdbcUrl;

    @Value("${tdengine.username:root}")
    private String tdengineUsername;

    @Value("${tdengine.password:taosdata}")
    private String tdenginePassword;

    @Value("${tdengine.super-table:device_data}")
    private String tdengineSuperTable;

    @Value("${spring.datasource.url:}")
    private String mysqlJdbcUrl;

    @Value("${spring.datasource.username:}")
    private String mysqlUsername;

    @Value("${spring.datasource.password:}")
    private String mysqlPassword;

    @Value("${mqtt.broker-url:}")
    private String mqttBrokerUrl;

    @Value("${mqtt.topic-pattern:devices/+/telemetry}")
    private String mqttTopicPattern;

    @Value("${mqtt.username:}")
    private String mqttUsername;

    @Value("${mqtt.password:}")
    private String mqttPasswordValue;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -----------------------------------------------------------------------
    // 查询
    // -----------------------------------------------------------------------

    @Override
    public Page<RuleConfigListItemDTO> getRuleConfigList(Page<RuleConfig> page, String ruleName, String algorithmId) {
        QueryWrapper<RuleConfig> qw = new QueryWrapper<>();
        if (ruleName != null && !ruleName.isEmpty()) {
            qw.like("rule_name", ruleName);
        }
        if (algorithmId != null && !algorithmId.isEmpty()) {
            qw.eq("algorithm_id", algorithmId);
        }
        qw.orderByDesc("create_time");

        Page<RuleConfig> configPage = this.page(page, qw);
        if ("flink".equalsIgnoreCase(engineType)) {
            configPage.getRecords().forEach(this::syncFlinkRuntimeStateIfNeeded);
        }

        List<String> algorithmIds = configPage.getRecords().stream()
                .map(RuleConfig::getAlgorithmId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        Map<String, RuleAlgorithm> algorithmMap = new HashMap<>();
        if (!algorithmIds.isEmpty()) {
            algorithmService.listByIds(algorithmIds)
                    .forEach(a -> algorithmMap.put(a.getId(), a));
        }

        List<RuleConfigListItemDTO> dtoList = configPage.getRecords().stream().map(config -> {
            RuleConfigListItemDTO dto = new RuleConfigListItemDTO();
            dto.setId(config.getId());
            dto.setRuleName(config.getRuleName());
            dto.setRuleDesc(config.getRuleDesc());
            dto.setAlgorithmId(config.getAlgorithmId());
            dto.setTriggerType(config.getTriggerType());
            dto.setTriggerCron(config.getTriggerCron());
            dto.setWindowType(config.getWindowType());
            dto.setWindowSize(config.getWindowSize());
            dto.setWindowUnit(config.getWindowUnit());
            dto.setKeyStrategy(config.getKeyStrategy());
            dto.setParallelism(config.getParallelism());
            dto.setRuleStatus(config.getRuleStatus());
            dto.setFlinkJobId(config.getFlinkJobId());
            dto.setCreateTime(config.getCreateTime());
            dto.setUpdateTime(config.getUpdateTime());
            RuleAlgorithm alg = algorithmMap.get(config.getAlgorithmId());
            if (alg != null) {
                dto.setAlgorithmName(alg.getAlgorithmName());
                dto.setAlgorithmType(alg.getAlgorithmType());
            }
            return dto;
        }).collect(Collectors.toList());

        Page<RuleConfigListItemDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), configPage.getTotal());
        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    public RuleConfigDetailDTO getRuleConfigDetail(String id) {
        RuleConfig config = this.getById(id);
        if (config == null) return null;
        if ("flink".equalsIgnoreCase(engineType)) {
            syncFlinkRuntimeStateIfNeeded(config);
        }

        RuleAlgorithm algorithm = algorithmService.getById(config.getAlgorithmId());

        List<RuleDataSource> dataSources = dataSourceMapper.selectList(
                new QueryWrapper<RuleDataSource>().eq("rule_id", id));
        List<RuleParam> params = paramMapper.selectList(
                new QueryWrapper<RuleParam>().eq("rule_id", id));

        RuleConfigDetailDTO dto = new RuleConfigDetailDTO();
        RuleConfigVO configVO = new RuleConfigVO();
        BeanUtils.copyProperties(config, configVO);
        dto.setRuleConfig(configVO);
        dto.setDataSources(dataSources.stream().map(item -> {
            RuleDataSourceVO vo = new RuleDataSourceVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList());
        dto.setParams(params.stream().map(item -> {
            RuleParamVO vo = new RuleParamVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList());
        if (algorithm != null) {
            dto.setAlgorithmName(algorithm.getAlgorithmName());
            dto.setAlgorithmType(algorithm.getAlgorithmType());
        }
        return dto;
    }

    // -----------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------

    @Override
    public boolean addRuleConfig(RuleConfig ruleConfig) {
        long count = this.count(new QueryWrapper<RuleConfig>().eq("rule_name", ruleConfig.getRuleName()));
        if (count > 0) {
            throw new RuntimeException("规则名称已存在：" + ruleConfig.getRuleName());
        }
        ruleConfig.setRuleStatus(RuleStatusEnum.STOPPED.getCode());
        if (ruleConfig.getKeyStrategy() == null) ruleConfig.setKeyStrategy("device_point");
        if (ruleConfig.getParallelism() == null) ruleConfig.setParallelism(2);
        Date now = new Date();
        ruleConfig.setCreateTime(now);
        ruleConfig.setUpdateTime(now);
        return this.save(ruleConfig);
    }

    @Override
    public boolean editRuleConfig(RuleConfig ruleConfig) {
        ruleConfig.setUpdateTime(new Date());
        return this.updateById(ruleConfig);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRuleConfig(String id) {
        RuleConfig config = this.getById(id);
        if (config != null && config.getRuleStatus() != null
                && config.getRuleStatus().equals(RuleStatusEnum.RUNNING.getCode())) {
            try {
                stopRule(id);
            } catch (Exception e) {
                log.warn("删除前停止规则失败: {}", e.getMessage());
            }
        }
        dataSourceMapper.delete(new QueryWrapper<RuleDataSource>().eq("rule_id", id));
        paramMapper.delete(new QueryWrapper<RuleParam>().eq("rule_id", id));
        executionLogService.clearLogsByRuleId(id);
        alarmService.clearAlarmsByRuleId(id);
        return this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDataSources(String ruleId, List<RuleDataSource> dataSources) {
        dataSourceMapper.delete(new QueryWrapper<RuleDataSource>().eq("rule_id", ruleId));
        if (dataSources != null && !dataSources.isEmpty()) {
            Date now = new Date();
            dataSources.forEach(ds -> {
                ds.setRuleId(ruleId);
                ds.setCreateTime(now);
                dataSourceMapper.insert(ds);
            });
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveParams(String ruleId, List<RuleParam> params) {
        paramMapper.delete(new QueryWrapper<RuleParam>().eq("rule_id", ruleId));
        if (params != null && !params.isEmpty()) {
            params.forEach(p -> {
                p.setRuleId(ruleId);
                paramMapper.insert(p);
            });
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // 规则启停（Flink 模式 / Local 模式双轨并行）
    // -----------------------------------------------------------------------

    @Override
    public boolean startRule(String id) {
        RuleConfig config = this.getById(id);
        if (config == null) throw new RuntimeException("规则不存在");
        if (config.getRuleStatus() != null && config.getRuleStatus().equals(RuleStatusEnum.RUNNING.getCode())) {
            throw new RuntimeException("规则已在运行中");
        }

        RuleAlgorithm algorithm = algorithmService.getById(config.getAlgorithmId());
        if (algorithm == null) throw new RuntimeException("规则绑定的算法不存在");
        if (algorithm.getAlgorithmStatus() == 0) throw new RuntimeException("算法已禁用，无法启动规则");

        List<RuleDataSource> dataSources = dataSourceMapper.selectList(
                new QueryWrapper<RuleDataSource>().eq("rule_id", id));
        List<RuleParam> params = paramMapper.selectList(
                new QueryWrapper<RuleParam>().eq("rule_id", id));

        if ("flink".equalsIgnoreCase(engineType)) {
            // ============ Flink 模式：提交 Job 到集群 ============
            RuleJobConfig jobConfig = buildRuleJobConfig(config, algorithm, dataSources, params);
            try {
                String flinkJobId = flinkJobManager.submitJob(jobConfig, true);
                config.setFlinkJobId(flinkJobId);
            } catch (Exception e) {
                throw new RuntimeException("提交 Flink Job 失败: " + e.getMessage(), e);
            }
        } else {
            // ============ Local 模式：线程池调度（开发调试） ============
            ruleEngineManager.startRule(config, algorithm, dataSources, params,
                    new RuleEngineManager.RuleExecutionLogConsumer() {
                        @Override
                        public void onCreate(RuleExecutionLog execLog) {
                            executionLogService.save(execLog);
                        }
                        @Override
                        public void onComplete(RuleExecutionLog execLog) {
                            executionLogService.updateById(execLog);
                        }
                    });
        }

        config.setRuleStatus(RuleStatusEnum.RUNNING.getCode());
        config.setUpdateTime(new Date());
        return this.updateById(config);
    }

    @Override
    public boolean stopRule(String id) {
        RuleConfig config = this.getById(id);
        if (config == null) throw new RuntimeException("规则不存在");

        if ("flink".equalsIgnoreCase(engineType)) {
            try {
                flinkJobManager.cancelJob(id, false);
            } catch (Exception e) {
                log.warn("取消 Flink Job 失败: {}", e.getMessage());
            }
        } else {
            ruleEngineManager.stopRule(id);
        }

        config.setRuleStatus(RuleStatusEnum.STOPPED.getCode());
        config.setFlinkJobId(null);
        config.setUpdateTime(new Date());
        return this.updateById(config);
    }

    @Override
    public boolean triggerOnce(String id) {
        RuleConfig config = this.getById(id);
        if (config == null) throw new RuntimeException("规则不存在");

        RuleAlgorithm algorithm = algorithmService.getById(config.getAlgorithmId());
        if (algorithm == null) throw new RuntimeException("规则绑定的算法不存在");

        List<RuleDataSource> dataSources = dataSourceMapper.selectList(
                new QueryWrapper<RuleDataSource>().eq("rule_id", id));
        List<RuleParam> params = paramMapper.selectList(
                new QueryWrapper<RuleParam>().eq("rule_id", id));

        if ("flink".equalsIgnoreCase(engineType)) {
            // Flink 模式下，triggerOnce 也提交一个一次性 Job
            RuleJobConfig jobConfig = buildRuleJobConfig(config, algorithm, dataSources, params);
            try {
                flinkJobManager.submitJob(jobConfig, false);
            } catch (Exception e) {
                throw new RuntimeException("触发 Flink Job 失败: " + e.getMessage(), e);
            }
        } else {
            ruleEngineManager.triggerOnce(config, algorithm, dataSources, params,
                    new RuleEngineManager.RuleExecutionLogConsumer() {
                        @Override
                        public void onCreate(RuleExecutionLog execLog) {
                            executionLogService.save(execLog);
                        }
                        @Override
                        public void onComplete(RuleExecutionLog execLog) {
                            executionLogService.updateById(execLog);
                        }
                    });
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // 构建 Flink Job 配置
    // -----------------------------------------------------------------------

    private RuleJobConfig buildRuleJobConfig(RuleConfig config,
                                              RuleAlgorithm algorithm,
                                              List<RuleDataSource> dataSources,
                                              List<RuleParam> params) {
        RuleJobConfig jobConfig = new RuleJobConfig();

        // 规则基础
        jobConfig.setRuleId(config.getId());
        jobConfig.setRuleName(config.getRuleName());
        jobConfig.setTriggerType(config.getTriggerType());

        // 窗口配置
        jobConfig.setWindowType(config.getWindowType());
        long windowMs = WindowUnitEnum.toMillis(
                config.getWindowSize() != null ? config.getWindowSize() : 60L,
                config.getWindowUnit());
        jobConfig.setWindowSizeMs(windowMs);
        jobConfig.setWindowSlideMs(config.getWindowSlide() != null
                ? WindowUnitEnum.toMillis(config.getWindowSlide(), config.getWindowUnit()) : 0L);

        // 分组策略和并行度
        jobConfig.setKeyStrategy(config.getKeyStrategy() != null ? config.getKeyStrategy() : "device_point");
        jobConfig.setParallelism(config.getParallelism() != null ? config.getParallelism() : 2);

        // 算法配置
        jobConfig.setAlgorithmType(algorithm.getAlgorithmType());
        jobConfig.setAlgorithmPath(resolveFlinkAlgorithmPath(algorithm.getAlgorithmPath()));
        jobConfig.setAlgorithmClass(algorithm.getAlgorithmClass());

        // 自定义参数
        Map<String, String> paramMap = buildRuleParamMap(params);
        if (!paramMap.isEmpty()) {
            jobConfig.setRuleParams(paramMap);
        }

        // 数据源
        if (dataSources != null) {
            List<RuleJobConfig.DataSourceConfig> dsConfigs = dataSources.stream().map(ds -> {
                RuleJobConfig.DataSourceConfig dsc = new RuleJobConfig.DataSourceConfig();
                dsc.setDeviceId(ds.getDeviceId());
                dsc.setDeviceName(ds.getDeviceName());
                dsc.setTimeRangeStart(ds.getTimeRangeStart());
                dsc.setTimeRangeEnd(ds.getTimeRangeEnd());
                // 解析 JSON 数组
                if (ds.getPointCodes() != null && !ds.getPointCodes().isEmpty()) {
                    try {
                        List<String> codes = objectMapper.readValue(ds.getPointCodes(),
                                new TypeReference<List<String>>() {});
                        dsc.setPointCodes(codes);
                    } catch (Exception e) {
                        dsc.setPointCodes(List.of(ds.getPointCodes()));
                    }
                }
                return dsc;
            }).collect(Collectors.toList());
            jobConfig.setDataSources(dsConfigs);
        }

        // 连接配置
        if (tdengineEnabled && tdengineJdbcUrl != null && !tdengineJdbcUrl.isBlank()) {
            RuleJobConfig.TDEngineConfig tdConfig = new RuleJobConfig.TDEngineConfig();
            tdConfig.setEnabled(true);
            tdConfig.setJdbcUrl(tdengineJdbcUrl);
            tdConfig.setUsername(tdengineUsername);
            tdConfig.setPassword(tdenginePassword);
            tdConfig.setSuperTable(tdengineSuperTable);
            tdConfig.setResultStable(tdengineResultStable);
            jobConfig.setTdengineConfig(tdConfig);
        }

        RuleJobConfig.MysqlConfig mysqlConfig = new RuleJobConfig.MysqlConfig();
        mysqlConfig.setJdbcUrl(mysqlJdbcUrl);
        mysqlConfig.setUsername(mysqlUsername);
        mysqlConfig.setPassword(mysqlPassword);
        jobConfig.setMysqlConfig(mysqlConfig);

        RuleJobConfig.MqttConfig mqtt = new RuleJobConfig.MqttConfig();
        mqtt.setBrokerUrl(mqttBrokerUrl);
        mqtt.setTopicPattern(mqttTopicPattern);
        mqtt.setUsername(mqttUsername);
        mqtt.setPassword(mqttPasswordValue);
        jobConfig.setMqttConfig(mqtt);

        RuleJobConfig.MockSourceConfig mockSourceConfig = buildMockSourceConfig(params);
        if (mockSourceConfig != null && mockSourceConfig.isEnabled()) {
            jobConfig.setMockSourceConfig(mockSourceConfig);
        }

        return jobConfig;
    }

    private Map<String, String> buildRuleParamMap(List<RuleParam> params) {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> paramMap = new HashMap<>();
        params.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getParamKey() != null && !item.getParamKey().startsWith("_mock_"))
                .forEach(p -> paramMap.put(p.getParamKey(), p.getParamValue()));
        return paramMap;
    }

    private RuleJobConfig.MockSourceConfig buildMockSourceConfig(List<RuleParam> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        Map<String, String> rawParams = new HashMap<>();
        params.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getParamKey() != null)
                .forEach(item -> rawParams.put(item.getParamKey(), item.getParamValue()));

        if (!Boolean.parseBoolean(rawParams.getOrDefault(MOCK_SOURCE_ENABLED_KEY, "false"))) {
            return null;
        }
        String eventsJson = rawParams.get(MOCK_SOURCE_EVENTS_JSON_KEY);
        if (eventsJson == null || eventsJson.isBlank()) {
            throw new RuntimeException("启用 mock source 时必须配置 " + MOCK_SOURCE_EVENTS_JSON_KEY);
        }
        try {
            List<RuleJobConfig.MockEvent> events = objectMapper.readValue(
                    eventsJson,
                    new TypeReference<List<RuleJobConfig.MockEvent>>() {
                    });
            if (events == null || events.isEmpty()) {
                throw new RuntimeException("mock source 事件列表不能为空");
            }
            RuleJobConfig.MockSourceConfig config = new RuleJobConfig.MockSourceConfig();
            config.setEnabled(true);
            config.setEvents(events);
            return config;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析 mock source 事件失败: " + e.getMessage(), e);
        }
    }

    private String resolveFlinkAlgorithmPath(String algorithmPath) {
        if (algorithmPath == null || algorithmPath.isBlank()) {
            return algorithmPath;
        }
        java.nio.file.Path originalPath = java.nio.file.Paths.get(algorithmPath);
        java.nio.file.Path fileName = originalPath.getFileName();
        if (fileName == null) {
            return algorithmPath;
        }
        java.nio.file.Path sharedPath = java.nio.file.Paths.get(sharedAlgorithmPath, fileName.toString());
        if (java.nio.file.Files.exists(sharedPath)) {
            return sharedPath.toString();
        }
        if (java.nio.file.Files.exists(originalPath)) {
            log.warn("Flink 算法共享路径不存在，回退到原始上传路径: sharedPath={}, originalPath={}",
                    sharedPath, originalPath);
            return originalPath.toString();
        }
        return sharedPath.toString();
    }

    private void syncFlinkRuntimeStateIfNeeded(RuleConfig config) {
        if (config == null) {
            return;
        }
        String flinkJobId = config.getFlinkJobId();
        Integer ruleStatus = config.getRuleStatus();
        if ((flinkJobId == null || flinkJobId.isBlank())
                && !Integer.valueOf(RuleStatusEnum.RUNNING.getCode()).equals(ruleStatus)) {
            return;
        }
        if (flinkJobId == null || flinkJobId.isBlank()) {
            if (Integer.valueOf(RuleStatusEnum.RUNNING.getCode()).equals(ruleStatus)) {
                config.setRuleStatus(RuleStatusEnum.STOPPED.getCode());
                config.setUpdateTime(new Date());
                this.updateById(config);
            }
            return;
        }

        flinkJobManager.registerJobMapping(config.getId(), flinkJobId);
        FlinkJobStatus status = flinkJobManager.getJobStatusByJobId(flinkJobId);
        if (status == FlinkJobStatus.RUNNING
                || status == FlinkJobStatus.CREATED
                || status == FlinkJobStatus.RESTARTING
                || status == FlinkJobStatus.RECONCILING) {
            if (!Integer.valueOf(RuleStatusEnum.RUNNING.getCode()).equals(ruleStatus)) {
                config.setRuleStatus(RuleStatusEnum.RUNNING.getCode());
                config.setUpdateTime(new Date());
                this.updateById(config);
            }
            return;
        }

        config.setRuleStatus(mapTerminalRuleStatus(status));
        config.setFlinkJobId(null);
        config.setUpdateTime(new Date());
        this.updateById(config);
        flinkJobManager.removeJobMapping(config.getId());
    }

    private Integer mapTerminalRuleStatus(FlinkJobStatus status) {
        if (status == FlinkJobStatus.FINISHED) {
            return RuleStatusEnum.COMPLETED.getCode();
        }
        if (status == FlinkJobStatus.FAILED
                || status == FlinkJobStatus.FAILING
                || status == FlinkJobStatus.SUSPENDED) {
            return RuleStatusEnum.FAILED.getCode();
        }
        return RuleStatusEnum.STOPPED.getCode();
    }
}
