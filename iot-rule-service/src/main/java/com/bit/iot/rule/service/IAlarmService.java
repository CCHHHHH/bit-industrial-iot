package com.bit.iot.rule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.rule.model.entity.AlarmRecord;
import com.bit.iot.rule.model.request.AlarmQueryRequest;
import com.bit.iot.rule.service.support.AlarmUpsertCommand;

/**
 * 告警服务。
 */
public interface IAlarmService extends IService<AlarmRecord> {

    Page<AlarmRecord> getAlarmList(Page<AlarmRecord> page, AlarmQueryRequest request);

    void createOrMergeAlarm(AlarmUpsertCommand command);

    boolean resolveAlarm(String id);

    boolean clearAlarmsByRuleId(String ruleId);
}
