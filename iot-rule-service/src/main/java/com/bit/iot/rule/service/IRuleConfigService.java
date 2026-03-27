package com.bit.iot.rule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.rule.model.dto.RuleConfigDetailDTO;
import com.bit.iot.rule.model.dto.RuleConfigListItemDTO;
import com.bit.iot.rule.model.entity.RuleConfig;
import com.bit.iot.rule.model.entity.RuleDataSource;
import com.bit.iot.rule.model.entity.RuleParam;

import java.util.List;

/**
 * 规则配置 Service
 *
 * @author chenhao
 * @since 2026-03-27
 */
public interface IRuleConfigService extends IService<RuleConfig> {

    /** 分页查询（携带算法名称） */
    Page<RuleConfigListItemDTO> getRuleConfigList(Page<RuleConfig> page, String ruleName, String algorithmId);

    /** 查询规则详情（含数据源和参数） */
    RuleConfigDetailDTO getRuleConfigDetail(String id);

    /** 新增规则（含重名校验） */
    boolean addRuleConfig(RuleConfig ruleConfig);

    /** 编辑规则 */
    boolean editRuleConfig(RuleConfig ruleConfig);

    /** 删除规则（同时删除数据源和参数） */
    boolean deleteRuleConfig(String id);

    /** 保存数据源配置（先删后存） */
    boolean saveDataSources(String ruleId, List<RuleDataSource> dataSources);

    /** 保存参数配置（先删后存） */
    boolean saveParams(String ruleId, List<RuleParam> params);

    /** 启动规则 */
    boolean startRule(String id);

    /** 停止规则 */
    boolean stopRule(String id);

    /** 手动触发执行一次 */
    boolean triggerOnce(String id);
}
