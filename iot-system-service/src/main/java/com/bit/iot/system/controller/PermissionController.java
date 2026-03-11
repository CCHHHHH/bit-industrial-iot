package com.bit.iot.system.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.system.model.entity.Permission;
import com.bit.iot.system.service.IPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 权限表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@RestController
@RequestMapping("/permission")
@Tag(name = "权限管理接口", description = "权限相关操作接口")
public class PermissionController extends BaseController {

    @Autowired
    private IPermissionService permissionService;

    @GetMapping("/list")
    @Operation(summary = "分页查询权限列表")
    public Result<List<Permission>> getPermissionList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String permissionName) {
        Page<Permission> page = new Page<>(current, size);
        Page<Permission> result = permissionService.getPermissionList(page, permissionName);
        return success(result);
    }

    @PostMapping
    @Operation(summary = "新增权限")
    public Result<Void> addPermission(@RequestBody Permission permission) {
        boolean success = permissionService.addPermission(permission);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑权限")
    public Result<Void> editPermission(@RequestBody Permission permission) {
        boolean success = permissionService.editPermission(permission);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{permissionId}")
    @Operation(summary = "删除权限")
    public Result<Void> deletePermission(@PathVariable String permissionId) {
        boolean success = permissionService.deletePermission(permissionId);
        return success ? success("删除成功") : error("删除失败");
    }
}
