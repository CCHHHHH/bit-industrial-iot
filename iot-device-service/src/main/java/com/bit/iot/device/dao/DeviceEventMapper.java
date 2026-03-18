package com.bit.iot.device.dao;

import com.bit.iot.device.model.entity.DeviceEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 设备事件表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Mapper
public interface DeviceEventMapper extends BaseMapper<DeviceEvent> {

}

