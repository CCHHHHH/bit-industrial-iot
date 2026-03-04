package com.bit.iot.system.dao;

import com.bit.iot.system.model.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 权限表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

}
