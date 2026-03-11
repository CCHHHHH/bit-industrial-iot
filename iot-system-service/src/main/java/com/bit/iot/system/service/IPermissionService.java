package com.bit.iot.system.service;

import com.bit.iot.system.model.entity.Permission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 权限表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
public interface IPermissionService extends IService<Permission> {

    /**
     * 分页查询权限列表（支持按权限名搜索）
     * @param page 分页对象
     * @param permissionName 权限名称（支持模糊查询）
     * @return 权限列表
     */
    Page<Permission> getPermissionList(Page<Permission> page, String permissionName);
    
    /**
     * 新增权限
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean addPermission(Permission permission);
    
    /**
     * 编辑权限
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean editPermission(Permission permission);
    
    /**
     * 删除权限（需要校验是否被角色所使用）
     * @param permissionId 权限 ID
     * @return 是否成功
     */
    boolean deletePermission(String permissionId);
    
}