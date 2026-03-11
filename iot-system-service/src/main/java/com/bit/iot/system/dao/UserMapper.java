package com.bit.iot.system.dao;

import com.bit.iot.system.model.dto.UserDto;
import com.bit.iot.system.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询用户列表（包含角色信息）
     * @param page 分页参数
     * @param username 用户名（支持模糊搜索）
     * @return 用户列表
     */
    Page<UserDto> selectUserListWithRoles(@Param("page") Page<UserDto> page, 
                                          @Param("username") String username);
}