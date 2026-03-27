package com.bit.iot.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.rule.dao.RuleExecutionLogMapper;
import com.bit.iot.rule.model.entity.RuleExecutionLog;
import com.bit.iot.rule.service.IRuleExecutionLogService;
import org.springframework.stereotype.Service;

/**
 * 规则执行日志 ServiceImpl
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Service
public class RuleExecutionLogServiceImpl extends ServiceImpl<RuleExecutionLogMapper, RuleExecutionLog>
        implements IRuleExecutionLogService {

    @Override
    public Page<RuleExecutionLog> getExecutionLogList(Page<RuleExecutionLog> page, String ruleId, Integer execStatus) {
        QueryWrapper<RuleExecutionLog> qw = new QueryWrapper<>();
        if (ruleId != null && !ruleId.isEmpty()) {
            qw.eq("rule_id", ruleId);
        }
        if (execStatus != null) {
            qw.eq("exec_status", execStatus);
        }
        qw.orderByDesc("start_time");
        return this.page(page, qw);
    }

    @Override
    public boolean clearLogsByRuleId(String ruleId) {
        return this.remove(new QueryWrapper<RuleExecutionLog>().eq("rule_id", ruleId));
    }
}
