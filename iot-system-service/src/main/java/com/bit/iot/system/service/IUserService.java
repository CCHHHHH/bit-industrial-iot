package com.bit.iot.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.system.model.dto.UserDto;
import com.bit.iot.system.model.entity.User;

import java.util.List;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
public interface IUserService extends IService<User> {
    
    /**
     * 获取用户列表
     * @param page 分页信息
     * @param username 用户名
     * @return 用户列表
     */
    Page<UserDto> getUserListWithRoles(Page<UserDto> page, String username);
    
    /**
     * 新增用户
     * @param user 用户信息
     * @return 是否成功
     */
    boolean addUser(User user);
    
    /**
     * 编辑用户
     * @param user 用户信息
     * @return 是否成功
     */
    boolean editUser(User user);
    
    /**
     * 修改密码
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 删除用户
     * @param userId 用户 ID
     * @return 是否成功
     */
    boolean deleteUser(Long userId);
    
    /**
     * 为用户分配角色
     * @param userId 用户 ID
     * @param roleIds 角色 ID 列表
     * @return 是否成功
     */
    boolean assignRolesToUser(String userId, List<String> roleIds);
    
    /**
     * 获取用户的角色 ID 列表
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    List<String> getRoleIdsByUserId(String userId);
    
    /**
     * 从用户中删除指定角色
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @return 是否成功
     */
    boolean removeRoleFromUser(String userId, String roleId);
    
}