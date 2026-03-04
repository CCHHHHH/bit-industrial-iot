package com.bit.iot.system.service.impl;

import com.bit.iot.system.model.entity.Role;
import com.bit.iot.system.dao.RoleMapper;
import com.bit.iot.system.service.IRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

}
