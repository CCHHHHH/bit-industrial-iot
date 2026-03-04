package com.bit.iot.system.dao;

import com.bit.iot.system.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
