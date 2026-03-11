package com.bit.iot.system.dao;

import com.bit.iot.system.model.entity.RolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 角色权限关联表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-09
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

}