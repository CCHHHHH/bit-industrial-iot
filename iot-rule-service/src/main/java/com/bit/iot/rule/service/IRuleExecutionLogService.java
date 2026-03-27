package com.bit.iot.rule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.rule.model.entity.RuleExecutionLog;

/**
 * 规则执行日志 Service
 *
 * @author chenhao
 * @since 2026-03-27
 */
public interface IRuleExecutionLogService extends IService<RuleExecutionLog> {

    /** 分页查询执行日志 */
    Page<RuleExecutionLog> getExecutionLogList(Page<RuleExecutionLog> page, String ruleId, Integer execStatus);

    /** 清空指定规则的执行日志 */
    boolean clearLogsByRuleId(String ruleId);
}
