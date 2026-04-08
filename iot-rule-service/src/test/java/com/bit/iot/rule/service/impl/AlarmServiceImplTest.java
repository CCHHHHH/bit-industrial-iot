package com.bit.iot.rule.service.impl;

import com.bit.iot.rule.dao.AlarmRecordMapper;
import com.bit.iot.rule.model.entity.AlarmRecord;
import com.bit.iot.rule.service.IAlarmService;
import com.bit.iot.rule.service.support.AlarmUpsertCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AlarmServiceImplTest.TestApplication.class)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:alarmtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:alarm-schema.sql",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.id-type=assign_uuid"
})
class AlarmServiceImplTest {

    @SpringBootApplication
    @MapperScan(basePackageClasses = AlarmRecordMapper.class)
    @Import(AlarmServiceImpl.class)
    static class TestApplication {
    }

    @Autowired
    private IAlarmService alarmService;

    @BeforeEach
    void setUp() {
        alarmService.remove(null);
    }

    @Test
    void createOrMergeAlarm_shouldMergeActiveAlarm() {
        AlarmUpsertCommand first = command("rule:rule-1:device-1:temp", "68.5");
        AlarmUpsertCommand second = command("rule:rule-1:device-1:temp", "69.3");

        alarmService.createOrMergeAlarm(first);
        alarmService.createOrMergeAlarm(second);

        assertThat(alarmService.list()).hasSize(1);
        AlarmRecord record = alarmService.list().getFirst();
        assertThat(record.getAlarmStatus()).isEqualTo("active");
        assertThat(record.getTriggerCount()).isEqualTo(2);
        assertThat(record.getMetricValue()).isEqualTo("69.3");
        assertThat(record.getAlarmMessage()).isEqualTo("温度超限");
    }

    @Test
    void resolveAlarm_thenCreateAgain_shouldCreateNewActiveAlarm() {
        AlarmUpsertCommand first = command("rule:rule-1:device-1:temp", "68.5");
        alarmService.createOrMergeAlarm(first);

        AlarmRecord existing = alarmService.list().getFirst();
        boolean resolved = alarmService.resolveAlarm(existing.getId());

        AlarmUpsertCommand second = command("rule:rule-1:device-1:temp", "71.2");
        alarmService.createOrMergeAlarm(second);

        assertThat(resolved).isTrue();
        assertThat(alarmService.list()).hasSize(2);
        long activeCount = alarmService.list().stream()
                .filter(item -> "active".equals(item.getAlarmStatus()))
                .count();
        long resolvedCount = alarmService.list().stream()
                .filter(item -> "resolved".equals(item.getAlarmStatus()))
                .count();
        assertThat(activeCount).isEqualTo(1);
        assertThat(resolvedCount).isEqualTo(1);
    }

    private AlarmUpsertCommand command(String dedupKey, String metricValue) {
        AlarmUpsertCommand command = new AlarmUpsertCommand();
        command.setSourceType("rule");
        command.setSourceId("rule-1");
        command.setRuleId("rule-1");
        command.setRuleName("温度阈值规则");
        command.setDeviceId("device-1");
        command.setDeviceName("温度传感器-001");
        command.setPointCode("temp");
        command.setDedupKey(dedupKey);
        command.setAlarmTitle("温度超限");
        command.setAlarmMessage("温度超限");
        command.setAlarmLevel("error");
        command.setMetricName("temperature");
        command.setMetricValue(metricValue);
        command.setResultData(Map.of("alert", true, "metricValue", metricValue));
        command.setTriggerTime(new Date());
        return command;
    }
}
