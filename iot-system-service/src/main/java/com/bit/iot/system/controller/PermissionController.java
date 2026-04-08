package com.bit.iot.system.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.system.model.request.PermissionRequest;
import com.bit.iot.system.model.entity.Permission;
import com.bit.iot.system.model.vo.PermissionVO;
import com.bit.iot.system.service.IPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
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
    public Result<List<PermissionVO>> getPermissionList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String permissionName) {
        Page<Permission> page = new Page<>(current, size);
        Page<Permission> result = permissionService.getPermissionList(page, permissionName);
        Page<PermissionVO> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    @PostMapping
    @Operation(summary = "新增权限")
    public Result<Void> addPermission(@RequestBody PermissionRequest permission) {
        Permission entity = new Permission();
        BeanUtils.copyProperties(permission, entity);
        boolean success = permissionService.addPermission(entity);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑权限")
    public Result<Void> editPermission(@RequestBody PermissionRequest permission) {
        Permission entity = new Permission();
        BeanUtils.copyProperties(permission, entity);
        boolean success = permissionService.editPermission(entity);
        return success ? success("修改成功") : error("修改失败");
    }

    private PermissionVO toVO(Permission permission) {
        PermissionVO vo = new PermissionVO();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }

    @DeleteMapping("/{permissionId}")
    @Operation(summary = "删除权限")
    public Result<Void> deletePermission(@PathVariable String permissionId) {
        boolean success = permissionService.deletePermission(permissionId);
        return success ? success("删除成功") : error("删除失败");
    }
}
