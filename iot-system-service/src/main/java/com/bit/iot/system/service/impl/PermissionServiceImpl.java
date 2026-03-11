package com.bit.iot.system.service.impl;

import com.bit.iot.system.model.entity.Permission;
import com.bit.iot.system.model.entity.RolePermission;
import com.bit.iot.system.dao.PermissionMapper;
import com.bit.iot.system.dao.RolePermissionMapper;
import com.bit.iot.system.service.IPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 权限表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public Page<Permission> getPermissionList(Page<Permission> page, String permissionName) {
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        
        if (permissionName != null && !permissionName.isEmpty()) {
            queryWrapper.like("permission_name", permissionName);
        }
        
        queryWrapper.orderByDesc("id");
        
        return this.page(page, queryWrapper);
    }

    @Override
    public boolean addPermission(Permission permission) {
        Date now = new Date();
        
        // 检查权限代码是否已存在
        QueryWrapper<Permission> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("permission_code", permission.getPermissionCode());
        Long count = this.count(checkWrapper);
        if (count > 0) {
            throw new RuntimeException("权限代码已存在");
        }
        
        return this.save(permission);
    }

    @Override
    public boolean editPermission(Permission permission) {
        Date now = new Date();
        
        // 检查权限代码是否已被其他权限使用
        QueryWrapper<Permission> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("permission_code", permission.getPermissionCode())
                    .ne("id", permission.getId());
        Long count = this.count(checkWrapper);
        if (count > 0) {
            throw new RuntimeException("权限代码已存在");
        }
        
        UpdateWrapper<Permission> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", permission.getId())
                    .set("permission_name", permission.getPermissionName())
                    .set("permission_code", permission.getPermissionCode())
                    .set("permission_desc", permission.getPermissionDesc())
                    .set("permission_type", permission.getPermissionType())
                    .set("update_time", now);
        
        return this.update(updateWrapper);
    }

    @Override
    public boolean deletePermission(String permissionId) {
        // 校验该权限是否被角色使用
        QueryWrapper<RolePermission> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("permission_id", permissionId);
        Long count = rolePermissionMapper.selectCount(checkWrapper);
        
        if (count > 0) {
            throw new RuntimeException("该权限已被角色使用，无法删除");
        }
        
        return this.removeById(permissionId);
    }

}