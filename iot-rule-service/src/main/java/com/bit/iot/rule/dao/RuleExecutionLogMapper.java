package com.bit.iot.rule.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bit.iot.rule.model.entity.RuleExecutionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则执行日志 Mapper
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Mapper
public interface RuleExecutionLogMapper extends BaseMapper<RuleExecutionLog> {
}
