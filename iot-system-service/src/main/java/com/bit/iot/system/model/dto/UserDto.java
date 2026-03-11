package com.bit.iot.system.model.dto;

import com.bit.iot.system.model.entity.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 *
 * @author chenhao
 * @date 2026/3/9 09:04
 *
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDto extends User {

    private List<RoleDto> roles;

}
