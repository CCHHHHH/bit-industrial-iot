package com.bit.iot.rule.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bit.iot.rule.model.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录 Mapper。
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
