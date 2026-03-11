package com.bit.iot.system.service;

import com.bit.iot.system.model.dto.RoleDto;
import com.bit.iot.system.model.entity.Role;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
public interface IRoleService extends IService<Role> {

    /**
     * 分页查询角色列表（包含权限信息）
     * @param page 分页对象
     * @return 角色 DTO 列表
     */
    Page<RoleDto> getRoleListWithPermissions(Page<RoleDto> page);
    
    /**
     * 分页查询角色列表（包含权限信息，支持角色名搜索）
     * @param page 分页对象
     * @param roleName 角色名称（支持模糊查询）
     * @return 角色 DTO 列表
     */
    Page<RoleDto> getRoleListWithPermissions(Page<RoleDto> page, String roleName);
    
    /**
     * 根据角色 ID 获取权限列表
     * @param roleId 角色 ID
     * @return 角色 DTO 列表（包含权限信息）
     */
    List<RoleDto> getPermissionsByRoleId(String roleId);
    
    /**
     * 新增角色
     * @param role 角色信息
     * @param permissionIds 权限 ID 列表
     * @return 是否成功
     */
    boolean addRole(Role role, List<String> permissionIds);
    
    /**
     * 修改角色
     * @param role 角色信息
     * @param permissionIds 权限 ID 列表
     * @return 是否成功
     */
    boolean editRole(Role role, List<String> permissionIds);
    
    /**
     * 删除角色
     * @param roleId 角色 ID
     * @return 是否成功
     */
    boolean deleteRole(String roleId);
    
    /**
     * 为角色分配权限
     * @param roleId 角色 ID
     * @param permissionIds 权限 ID 列表
     * @return 是否成功
     */
    boolean assignPermissionsToRole(String roleId, List<String> permissionIds);
    
    /**
     * 获取角色的权限 ID 列表
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    List<String> getPermissionIdsByRoleId(String roleId);
    
    /**
     * 删除角色上的指定权限
     * @param roleId 角色 ID
     * @param permissionId 权限 ID
     * @return 是否成功
     */
    boolean removePermissionFromRole(String roleId, String permissionId);
    
}