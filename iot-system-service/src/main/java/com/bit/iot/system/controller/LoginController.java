package com.bit.iot.system.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.bit.iot.security.annotation.RequirePermission;
import com.bit.iot.security.context.UserContextHolder;
import com.bit.iot.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@RestController
@RequestMapping("/login")
@Tag(name = "用户登录管理", description = "用户登录管理")
public class LoginController extends BaseController {

    @Autowired
    private IUserService userService;



    @GetMapping("/current")
    @Operation(summary = "获取当前登录用户")
    @RequirePermission(requireLogin = true)
    public Result<String> getCurrentUserInfo() {
        // 从 ThreadLocal 中获取用户信息
        var context = UserContextHolder.getContext();
        if (context != null) {
            return success("当前用户：" + context.getUsername());
        }
        return error("未获取到用户信息");
    }
}
