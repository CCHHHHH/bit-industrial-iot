package com.bit.iot.rule.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bit.iot.rule.model.entity.RuleConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则配置 Mapper
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Mapper
public interface RuleConfigMapper extends BaseMapper<RuleConfig> {
}
