package com.bit.iot.system.model.dto;

import com.bit.iot.system.model.entity.Permission;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限 DTO（扩展 roleId 字段用于关联查询）
 *
 * @author chenhao
 * @date 2026/3/30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PermissionDto extends Permission {

    /**
     * 角色 ID（用于权限与角色的关联）
     */
    private String roleId;

}
