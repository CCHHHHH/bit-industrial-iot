package com.bit.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.system.dao.RoleMapper;
import com.bit.iot.system.dao.RolePermissionMapper;
import com.bit.iot.system.model.dto.PermissionDto;
import com.bit.iot.system.model.dto.RoleDto;
import com.bit.iot.system.model.entity.Permission;
import com.bit.iot.system.model.entity.Role;
import com.bit.iot.system.model.entity.RolePermission;
import com.bit.iot.system.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
        return getRoleListWithPermissions(page, null);
    }

    @Override
    public Page<RoleDto> getRoleListWithPermissions(Page<RoleDto> page, String roleName) {
        // 1. 先分页查询角色列表（不包含权限）
        Page<RoleDto> rolePage = this.getBaseMapper().selectRolePage(page, roleName);
        
        // 2. 如果角色列表为空，直接返回
        if (rolePage.getRecords() == null || rolePage.getRecords().isEmpty()) {
            return rolePage;
        }
        
        // 3. 提取所有角色 ID
        List<String> roleIds = rolePage.getRecords().stream()
                .map(Role::getId)
                .collect(Collectors.toList());
        
        // 4. 批量查询这些角色的权限信息
        List<PermissionDto> allPermissions = this.getBaseMapper().selectPermissionsByRoleIds(roleIds);
        
        // 5. 按角色 ID 分组权限
        Map<String, List<Permission>> permissionMap = new HashMap<>();
        for (PermissionDto perm : allPermissions) {
            permissionMap.computeIfAbsent(perm.getRoleId(), k -> new ArrayList<>()).add(perm);
        }
        
        // 6. 为每个角色分配权限
        for (RoleDto role : rolePage.getRecords()) {
            List<Permission> permissions = permissionMap.getOrDefault(role.getId(), new ArrayList<>());
            role.setPermissions(permissions);
        }
        
        return rolePage;
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
        // 检查角色名称是否已存在
        QueryWrapper<Role> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("role_name", role.getRoleName());
        Long count = this.count(checkWrapper);
        if (count > 0) {
            throw new RuntimeException("角色名称已存在");
        }
        
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
        // 检查角色名称是否已被其他角色使用
        QueryWrapper<Role> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("role_name", role.getRoleName())
                    .ne("id", role.getId());
        Long count = this.count(checkWrapper);
        if (count > 0) {
            throw new RuntimeException("角色名称已存在");
        }
        
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