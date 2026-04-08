package com.bit.iot.rule.engine;

import com.bit.iot.common.flink.AlgorithmResult;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.rule.client.DataServiceClient;
import com.bit.iot.rule.model.entity.RuleAlgorithm;
import com.bit.iot.rule.model.entity.RuleConfig;
import com.bit.iot.rule.model.entity.RuleDataSource;
import com.bit.iot.rule.model.entity.RuleExecutionLog;
import com.bit.iot.rule.service.IAlarmService;
import com.bit.iot.rule.service.support.AlarmUpsertCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineManagerLocalFlowTest {

    @Mock
    private AlgorithmLoader algorithmLoader;

    @Mock
    private DataServiceClient dataServiceClient;

    @Mock
    private IAlarmService alarmService;

    private RuleEngineManager ruleEngineManager;

    @BeforeEach
    void setUp() {
        ruleEngineManager = new RuleEngineManager();
        ReflectionTestUtils.setField(ruleEngineManager, "algorithmLoader", algorithmLoader);
        ReflectionTestUtils.setField(ruleEngineManager, "dataServiceClient", dataServiceClient);
        ReflectionTestUtils.setField(ruleEngineManager, "alarmService", alarmService);
    }

    @Test
    void executeRule_shouldWriteSuccessLogAndCreateAlarm() throws Exception {
        RuleConfig ruleConfig = new RuleConfig();
        ruleConfig.setId("rule-1");
        ruleConfig.setRuleName("温度阈值规则");
        ruleConfig.setKeyStrategy("device_point");
        ruleConfig.setWindowSize(60L);
        ruleConfig.setWindowUnit("s");

        RuleAlgorithm algorithm = new RuleAlgorithm();
        algorithm.setId("alg-1");
        algorithm.setAlgorithmType("jar");

        RuleDataSource dataSource = new RuleDataSource();
        dataSource.setDeviceId("device-1");
        dataSource.setDeviceName("温度传感器-001");
        dataSource.setPointCodes("[\"temp\"]");
        List<RuleDataSource> dataSources = List.of(dataSource);

        List<DataPoint> points = List.of(
                new DataPoint("device-1", "temp", new Date(1_000L), 68.5, 0),
                new DataPoint("device-1", "temp", new Date(2_000L), 71.2, 0)
        );
        when(dataServiceClient.queryRuleWindow(anyList(), anyLong(), anyLong(), anyInt())).thenReturn(points);
        when(algorithmLoader.execute(any(RuleAlgorithm.class), anyList(), anyMap())).thenReturn(
                AlgorithmResult.success(Map.of(
                        "alert", true,
                        "alertLevel", "error",
                        "alertMessage", "温度超限",
                        "metricName", "temperature",
                        "metricValue", "71.2"
                ))
        );

        List<RuleExecutionLog> createdLogs = new ArrayList<>();
        List<RuleExecutionLog> completedLogs = new ArrayList<>();

        ReflectionTestUtils.invokeMethod(
                ruleEngineManager,
                "executeRule",
                "rule-1",
                ruleConfig,
                algorithm,
                dataSources,
                Map.of("threshold", "70"),
                new RuleEngineManager.RuleExecutionLogConsumer() {
                    @Override
                    public void onCreate(RuleExecutionLog log) {
                        createdLogs.add(log);
                    }

                    @Override
                    public void onComplete(RuleExecutionLog log) {
                        completedLogs.add(log);
                    }
                }
        );

        assertThat(createdLogs).hasSize(1);
        assertThat(completedLogs).hasSize(1);
        RuleExecutionLog completed = completedLogs.getFirst();
        assertThat(completed.getExecStatus()).isEqualTo(1);
        assertThat(completed.getWindowKey()).isEqualTo("device-1#temp");
        assertThat(completed.getResultData()).contains("\"alert\":true");

        ArgumentCaptor<AlarmUpsertCommand> alarmCaptor = ArgumentCaptor.forClass(AlarmUpsertCommand.class);
        verify(alarmService).createOrMergeAlarm(alarmCaptor.capture());
        AlarmUpsertCommand command = alarmCaptor.getValue();
        assertThat(command.getRuleId()).isEqualTo("rule-1");
        assertThat(command.getDeviceId()).isEqualTo("device-1");
        assertThat(command.getPointCode()).isEqualTo("temp");
        assertThat(command.getAlarmLevel()).isEqualTo("error");
        assertThat(command.getAlarmMessage()).isEqualTo("温度超限");
        assertThat(command.getDedupKey()).isEqualTo("rule:rule-1:device-1:temp");
    }
}
