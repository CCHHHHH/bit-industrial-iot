package com.bit.iot.integration.dao;

import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 集成实例数据映射表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@Mapper
public interface IntegrationDataMappingMapper extends BaseMapper<IntegrationDataMapping> {

}

