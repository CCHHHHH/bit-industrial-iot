package com.bit.iot.system.dao;

import com.bit.iot.system.model.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户角色关联表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-09
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

}