package com.bit.iot.system.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.system.model.dto.RoleDto;
import com.bit.iot.system.model.entity.Role;
import com.bit.iot.system.model.request.RoleRequest;
import com.bit.iot.system.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@RestController
@RequestMapping("/role")
@Tag(name = "角色管理接口", description = "角色相关操作接口")
public class RoleController extends BaseController {

    @Autowired
    private IRoleService roleService;

    @GetMapping("/list")
    @Operation(summary = "分页查询角色列表（包含权限信息）")
    public Result<List<RoleDto>> getRoleList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String roleName) {
        Page<RoleDto> page = new Page<>(current, size);
        Page<RoleDto> result = roleService.getRoleListWithPermissions(page, roleName);
        return success(result);
    }

    @GetMapping("/permissions/{roleId}")
    @Operation(summary = "根据角色 ID 获取权限列表")
    public Result<List<RoleDto>> getPermissionsByRoleId(@PathVariable String roleId) {
        List<RoleDto> permissions = roleService.getPermissionsByRoleId(roleId);
        return success(permissions);
    }

    @PostMapping
    @Operation(summary = "新增角色")
    public Result<Void> addRole(
            @RequestBody RoleRequest role,
            @RequestParam(required = false) List<String> permissionIds) {
        Role entity = new Role();
        BeanUtils.copyProperties(role, entity);
        boolean success = roleService.addRole(entity, permissionIds);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "修改角色")
    public Result<Void> editRole(
            @RequestBody RoleRequest role,
            @RequestParam(required = false) List<String> permissionIds) {
        Role entity = new Role();
        BeanUtils.copyProperties(role, entity);
        boolean success = roleService.editRole(entity, permissionIds);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "删除角色")
    public Result<Void> deleteRole(@PathVariable String roleId) {
        boolean success = roleService.deleteRole(roleId);
        return success ? success("删除成功") : error("删除失败");
    }

    @PostMapping("/permissions")
    @Operation(summary = "为角色分配权限")
    public Result<Void> assignPermissionsToRole(
            @RequestParam String roleId,
            @RequestBody List<String> permissionIds) {
        boolean success = roleService.assignPermissionsToRole(roleId, permissionIds);
        return success ? success("权限分配成功") : error("权限分配失败");
    }

    @GetMapping("/permissions/list/{roleId}")
    @Operation(summary = "获取角色的权限 ID 列表")
    public Result<List<String>> getPermissionIdsByRoleId(@PathVariable String roleId) {
        List<String> permissionIds = roleService.getPermissionIdsByRoleId(roleId);
        return success(permissionIds);
    }

    @DeleteMapping("/permissions")
    @Operation(summary = "删除角色上的指定权限")
    public Result<Void> removePermissionFromRole(
            @RequestParam String roleId,
            @RequestParam String permissionId) {
        boolean success = roleService.removePermissionFromRole(roleId, permissionId);
        return success ? success("权限移除成功") : error("权限移除失败");
    }
}
