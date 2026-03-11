package com.bit.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.system.dao.RoleMapper;
import com.bit.iot.system.dao.RolePermissionMapper;
import com.bit.iot.system.model.dto.RoleDto;
import com.bit.iot.system.model.entity.Permission;
import com.bit.iot.system.model.entity.Role;
import com.bit.iot.system.model.entity.RolePermission;
import com.bit.iot.system.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public Page<RoleDto> getRoleListWithPermissions(Page<RoleDto> page) {
        return this.getBaseMapper().selectRoleListWithPermissions(page, null);
    }

    @Override
    public Page<RoleDto> getRoleListWithPermissions(Page<RoleDto> page, String roleName) {
        return this.getBaseMapper().selectRoleListWithPermissions(page, roleName);
    }

    @Override
    public List<RoleDto> getPermissionsByRoleId(String roleId) {
        // 查询角色权限关联信息
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new QueryWrapper<RolePermission>()
                .eq("role_id", roleId)
        );
        
        // 转换为 RoleDto 列表（这里实际返回的是权限信息）
        return rolePermissions.stream().map(rolePermission -> {
            RoleDto roleDto = new RoleDto();
            // 这里可以进一步查询权限详细信息
            // 暂时只返回权限 ID
            Permission permission = new Permission();
            permission.setId(rolePermission.getPermissionId());
            roleDto.setPermissions(List.of(permission));
            return roleDto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRole(Role role, List<String> permissionIds) {
        Date now = new Date();
        role.setCreateTime(now);
        role.setUpdateTime(now);
        
        // 保存角色
        boolean saved = this.save(role);
        if (!saved) {
            throw new RuntimeException("新增角色失败");
        }
        
        // 保存角色权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (String permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(role.getId());
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editRole(Role role, List<String> permissionIds) {
        Date now = new Date();
        role.setUpdateTime(now);
        
        // 更新角色信息
        UpdateWrapper<Role> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", role.getId())
                    .set("role_name", role.getRoleName())
                    .set("update_time", now);
        
        boolean updated = this.update(updateWrapper);
        if (!updated) {
            throw new RuntimeException("修改角色失败");
        }
        
        // 删除原有的权限关联
        QueryWrapper<RolePermission> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("role_id", role.getId());
        rolePermissionMapper.delete(deleteWrapper);
        
        // 重新添加权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (String permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(role.getId());
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(String roleId) {
        // 删除角色权限关联
        QueryWrapper<RolePermission> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("role_id", roleId);
        rolePermissionMapper.delete(deleteWrapper);
        
        // 删除角色
        return this.removeById(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissionsToRole(String roleId, List<String> permissionIds) {
        // 删除原有的权限关联
        QueryWrapper<RolePermission> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("role_id", roleId);
        rolePermissionMapper.delete(deleteWrapper);
        
        // 添加新的权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (String permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
        
        return true;
    }

    @Override
    public List<String> getPermissionIdsByRoleId(String roleId) {
        QueryWrapper<RolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(queryWrapper);
        
        return rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePermissionFromRole(String roleId, String permissionId) {
        QueryWrapper<RolePermission> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("role_id", roleId)
                    .eq("permission_id", permissionId);
        
        int deleted = rolePermissionMapper.delete(deleteWrapper);
        
        return deleted > 0;
    }

}