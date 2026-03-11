package com.bit.iot.system.service.impl;

import bit.iot.common.utils.MD5Util;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.system.dao.UserMapper;
import com.bit.iot.system.model.dto.UserDto;
import com.bit.iot.system.model.entity.User;
import com.bit.iot.system.model.entity.UserRole;
import com.bit.iot.system.dao.UserRoleMapper;
import com.bit.iot.system.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public Page<UserDto> getUserListWithRoles(Page<UserDto> page, String username) {
        // 获取用户列表
        Page<UserDto> userPage = new Page<>(page.getCurrent(), page.getSize());
        return this.getBaseMapper().selectUserListWithRoles(userPage, username);
    }

    @Override
    public boolean addUser(User user) {
        // 设置默认时间
        Date now = new Date();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        user.setPassword(MD5Util.md5WithSalt(user.getPassword(), user.getUsername()));

        // 插入数据库
        return this.save(user);
    }

    @Override
    public boolean editUser(User user) {
        Date now = new Date();
        
        // 只更新用户名和手机号，不更新密码
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", user.getId())
                    .set("username", user.getUsername())
                    .set("phone_number", user.getPhoneNumber())
                    .set("update_time", now);
        
        return this.update(updateWrapper);
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        String encryptedOldPassword = MD5Util.md5WithSalt(oldPassword, user.getUsername());
        if (!encryptedOldPassword.equals(user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 加密新密码
        String encryptedNewPassword = MD5Util.md5WithSalt(newPassword, user.getUsername());
        
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                    .set("password", encryptedNewPassword)
                    .set("update_time", new Date());
        
        return this.update(updateWrapper);
    }

    @Override
    public boolean deleteUser(Long userId) {
        return this.removeById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(String userId, List<String> roleIds) {
        // 删除用户原有的角色关联
        QueryWrapper<UserRole> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("user_id", userId);
        userRoleMapper.delete(deleteWrapper);
        
        // 添加新的角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            for (String roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
        
        return true;
    }

    @Override
    public List<String> getRoleIdsByUserId(String userId) {
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        
        List<UserRole> userRoles = userRoleMapper.selectList(queryWrapper);
        
        return userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRoleFromUser(String userId, String roleId) {
        QueryWrapper<UserRole> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("user_id", userId)
                    .eq("role_id", roleId);
        
        int deleted = userRoleMapper.delete(deleteWrapper);
        
        return deleted > 0;
    }

}