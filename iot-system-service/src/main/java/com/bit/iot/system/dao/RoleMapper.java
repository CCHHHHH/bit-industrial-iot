package com.bit.iot.system.dao;

import com.bit.iot.system.model.dto.RoleDto;
import com.bit.iot.system.model.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    
    /**
     * 分页查询角色列表（包含权限信息）
     * @param page 分页对象
     * @param roleName 角色名称（支持模糊查询）
     * @return 角色 DTO 列表
     */
    Page<RoleDto> selectRoleListWithPermissions(Page<RoleDto> page, @Param("roleName") String roleName);
    
}
