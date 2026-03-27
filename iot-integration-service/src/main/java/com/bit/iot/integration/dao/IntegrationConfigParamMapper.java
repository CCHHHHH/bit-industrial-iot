package com.bit.iot.integration.dao;

import com.bit.iot.integration.model.entity.IntegrationConfigParam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 集成实例配置参数表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-20 11:32:38
 */
@Mapper
public interface IntegrationConfigParamMapper extends BaseMapper<IntegrationConfigParam> {

}

