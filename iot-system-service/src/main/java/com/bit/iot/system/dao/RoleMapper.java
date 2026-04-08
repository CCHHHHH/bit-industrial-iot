package com.bit.iot.system.dao;

import com.bit.iot.system.model.dto.PermissionDto;
import com.bit.iot.system.model.dto.RoleDto;
import com.bit.iot.system.model.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
     * 分页查询角色列表（不包含权限，用于分页）
     * @param page 分页对象
     * @param roleName 角色名称（支持模糊查询）
     * @return 角色 DTO 列表
     */
    Page<RoleDto> selectRolePage(Page<RoleDto> page, @Param("roleName") String roleName);
    
    /**
     * 批量查询角色的权限信息
     * @param roleIds 角色 ID 列表
     * @return 权限 DTO 列表（包含 roleId）
     */
    List<PermissionDto> selectPermissionsByRoleIds(@Param("roleIds") List<String> roleIds);
    
}
