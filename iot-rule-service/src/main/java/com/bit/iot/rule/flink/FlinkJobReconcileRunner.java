package com.bit.iot.rule.flink;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bit.iot.rule.model.entity.RuleConfig;
import com.bit.iot.rule.service.IRuleConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FlinkJobReconcileRunner {

    private final IRuleConfigService ruleConfigService;
    private final FlinkJobManager flinkJobManager;

    public FlinkJobReconcileRunner(IRuleConfigService ruleConfigService, FlinkJobManager flinkJobManager) {
        this.ruleConfigService = ruleConfigService;
        this.flinkJobManager = flinkJobManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcile() {
        List<RuleConfig> configs = ruleConfigService.list(new QueryWrapper<RuleConfig>()
                .isNotNull("flink_job_id")
                .or()
                .eq("rule_status", 1));
        for (RuleConfig config : configs) {
            try {
                reconcileOne(config);
            } catch (Exception e) {
                log.warn("Flink 任务状态对账失败: ruleId={}, error={}", config.getId(), e.getMessage());
            }
        }
    }

    private void reconcileOne(RuleConfig config) {
        if (config.getFlinkJobId() == null || config.getFlinkJobId().isBlank()) {
            if (Integer.valueOf(1).equals(config.getRuleStatus())) {
                config.setRuleStatus(0);
                ruleConfigService.updateById(config);
            }
            return;
        }

        flinkJobManager.registerJobMapping(config.getId(), config.getFlinkJobId());
        FlinkJobStatus status = flinkJobManager.getJobStatus(config.getId());
        if (status == FlinkJobStatus.RUNNING
                || status == FlinkJobStatus.CREATED
                || status == FlinkJobStatus.RESTARTING
                || status == FlinkJobStatus.RECONCILING) {
            if (!Integer.valueOf(1).equals(config.getRuleStatus())) {
                config.setRuleStatus(1);
                ruleConfigService.updateById(config);
            }
            return;
        }

        config.setRuleStatus(0);
        config.setFlinkJobId(null);
        ruleConfigService.updateById(config);
        flinkJobManager.removeJobMapping(config.getId());
    }
}
