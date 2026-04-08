package com.bit.iot.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.rule.dao.AlarmRecordMapper;
import com.bit.iot.rule.model.entity.AlarmRecord;
import com.bit.iot.rule.model.request.AlarmQueryRequest;
import com.bit.iot.rule.service.IAlarmService;
import com.bit.iot.rule.service.support.AlarmUpsertCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 告警服务实现。
 */
@Service
public class AlarmServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord> implements IAlarmService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_RESOLVED = "resolved";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Page<AlarmRecord> getAlarmList(Page<AlarmRecord> page, AlarmQueryRequest request) {
        QueryWrapper<AlarmRecord> queryWrapper = new QueryWrapper<>();
        if (request != null) {
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String keyword = request.getKeyword().trim();
                queryWrapper.and(wrapper -> wrapper
                        .like("device_name", keyword)
                        .or()
                        .like("alarm_message", keyword)
                        .or()
                        .like("rule_name", keyword));
            }
            if (request.getAlarmStatus() != null && !request.getAlarmStatus().isBlank()) {
                queryWrapper.eq("alarm_status", request.getAlarmStatus().trim());
            }
            if (request.getAlarmLevel() != null && !request.getAlarmLevel().isBlank()) {
                queryWrapper.eq("alarm_level", normalizeLevel(request.getAlarmLevel()));
            }
        }
        queryWrapper.orderByDesc("last_trigger_time").orderByDesc("update_time");
        return this.page(page, queryWrapper);
    }

    @Override
    public void createOrMergeAlarm(AlarmUpsertCommand command) {
        if (command == null || command.getDedupKey() == null || command.getDedupKey().isBlank()) {
            return;
        }
        try {
            doCreateOrMerge(command);
        } catch (DuplicateKeyException duplicateKeyException) {
            doCreateOrMerge(command);
        }
    }

    @Override
    public boolean resolveAlarm(String id) {
        AlarmRecord existing = this.getById(id);
        if (existing == null) {
            return false;
        }
        if (STATUS_RESOLVED.equals(existing.getAlarmStatus())) {
            return true;
        }
        Date now = new Date();
        existing.setAlarmStatus(STATUS_RESOLVED);
        existing.setResolvedTime(now);
        existing.setUpdateTime(now);
        return this.updateById(existing);
    }

    @Override
    public boolean clearAlarmsByRuleId(String ruleId) {
        return this.remove(new QueryWrapper<AlarmRecord>().eq("rule_id", ruleId));
    }

    private void doCreateOrMerge(AlarmUpsertCommand command) {
        AlarmRecord existing = this.lambdaQuery()
                .eq(AlarmRecord::getDedupKey, command.getDedupKey())
                .eq(AlarmRecord::getAlarmStatus, STATUS_ACTIVE)
                .one();

        Date triggerTime = command.getTriggerTime() != null ? command.getTriggerTime() : new Date();
        if (existing == null) {
            AlarmRecord record = new AlarmRecord();
            record.setSourceType(defaultValue(command.getSourceType(), "rule"));
            record.setSourceId(defaultValue(command.getSourceId(), command.getRuleId()));
            record.setRuleId(command.getRuleId());
            record.setRuleName(command.getRuleName());
            record.setDeviceId(command.getDeviceId());
            record.setDeviceName(firstNonBlank(command.getDeviceName(), command.getDeviceId()));
            record.setPointCode(command.getPointCode());
            record.setDedupKey(command.getDedupKey());
            record.setAlarmTitle(firstNonBlank(command.getAlarmTitle(), command.getAlarmMessage(), defaultMessage(command)));
            record.setAlarmMessage(firstNonBlank(command.getAlarmMessage(), defaultMessage(command)));
            record.setAlarmLevel(normalizeLevel(command.getAlarmLevel()));
            record.setAlarmStatus(STATUS_ACTIVE);
            record.setTriggerCount(1);
            record.setFirstTriggerTime(triggerTime);
            record.setLastTriggerTime(triggerTime);
            record.setMetricName(command.getMetricName());
            record.setMetricValue(command.getMetricValue());
            record.setResultData(writeResultData(command));
            record.setCreateTime(triggerTime);
            record.setUpdateTime(triggerTime);
            this.save(record);
            return;
        }

        existing.setRuleName(command.getRuleName());
        existing.setDeviceName(firstNonBlank(command.getDeviceName(), existing.getDeviceName(), existing.getDeviceId()));
        existing.setAlarmTitle(firstNonBlank(command.getAlarmTitle(), command.getAlarmMessage(), existing.getAlarmTitle(), defaultMessage(command)));
        existing.setAlarmMessage(firstNonBlank(command.getAlarmMessage(), existing.getAlarmMessage(), defaultMessage(command)));
        existing.setAlarmLevel(normalizeLevel(command.getAlarmLevel()));
        existing.setTriggerCount((existing.getTriggerCount() == null ? 0 : existing.getTriggerCount()) + 1);
        existing.setLastTriggerTime(triggerTime);
        existing.setMetricName(firstNonBlank(command.getMetricName(), existing.getMetricName()));
        existing.setMetricValue(firstNonBlank(command.getMetricValue(), existing.getMetricValue()));
        String resultData = writeResultData(command);
        if (resultData != null) {
            existing.setResultData(resultData);
        }
        existing.setUpdateTime(triggerTime);
        this.updateById(existing);
    }

    private String writeResultData(AlarmUpsertCommand command) {
        if (command.getResultData() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(command.getResultData());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "warning";
        }
        String normalized = level.trim().toLowerCase();
        return switch (normalized) {
            case "info", "warning", "error" -> normalized;
            case "warn" -> "warning";
            case "err" -> "error";
            default -> "warning";
        };
    }

    private String defaultMessage(AlarmUpsertCommand command) {
        String ruleName = command.getRuleName() == null || command.getRuleName().isBlank()
                ? "规则"
                : command.getRuleName().trim();
        return ruleName + "触发告警";
    }

    private String defaultValue(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
