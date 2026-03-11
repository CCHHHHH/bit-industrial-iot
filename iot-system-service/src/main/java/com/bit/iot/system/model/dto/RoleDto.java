package com.bit.iot.system.model.dto;

import com.bit.iot.system.model.entity.Permission;
import com.bit.iot.system.model.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 *
 * @author chenhao
 * @date 2026/3/9 09:05
 *
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleDto extends Role {

    private List<Permission> permissions;

}
