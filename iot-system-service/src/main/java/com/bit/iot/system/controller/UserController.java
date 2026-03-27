package com.bit.iot.system.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.security.annotation.RequirePermission;
import com.bit.iot.security.context.UserContextHolder;
import com.bit.iot.system.model.dto.UserResponseDto;
import com.bit.iot.system.model.entity.User;
import com.bit.iot.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理接口", description = "用户相关操作接口")
public class UserController extends BaseController {

    @Autowired
    private IUserService userService;

    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表")
    public Result<List<UserResponseDto>> getUserList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String username) {
        Page<UserResponseDto> page = new Page<>(current, size);
        Page<UserResponseDto> result = userService.getUserListWithRoles(page, username);
        return success(result);
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public Result<Void> addUser(@RequestBody User user) {
        boolean success = userService.addUser(user);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑用户")
    public Result<Void> editUser(@RequestBody User user) {
        boolean success = userService.editUser(user);
        return success ? success("修改成功") : error("修改失败");
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(
            @RequestParam Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        boolean success = userService.changePassword(userId, oldPassword, newPassword);
        return success ? success("密码修改成功") : error("密码修改失败");
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        boolean success = userService.deleteUser(userId);
        return success ? success("删除成功") : error("删除失败");
    }

    @PostMapping("/roles")
    @Operation(summary = "为用户分配角色")
    public Result<Void> assignRolesToUser(
            @RequestParam String userId,
            @RequestBody List<String> roleIds) {
        boolean success = userService.assignRolesToUser(userId, roleIds);
        return success ? success("角色分配成功") : error("角色分配失败");
    }

    @GetMapping("/roles/{userId}")
    @Operation(summary = "获取用户的角色 ID 列表")
    public Result<List<String>> getRoleIdsByUserId(@PathVariable String userId) {
        List<String> roleIds = userService.getRoleIdsByUserId(userId);
        return success(roleIds);
    }

    @DeleteMapping("/roles")
    @Operation(summary = "从用户中删除指定角色")
    public Result<Void> removeRoleFromUser(
            @RequestParam String userId,
            @RequestParam String roleId) {
        boolean success = userService.removeRoleFromUser(userId, roleId);
        return success ? success("角色移除成功") : error("角色移除失败");
    }
}
