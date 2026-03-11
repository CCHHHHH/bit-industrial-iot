package com.bit.iot.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 角色权限关联表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-09
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("role_permission")
@Schema(description = "角色权限关联实体")
public class RolePermission {

    @TableId("id")
    private String id;

    @Schema(description = "角色ID")
    @TableField("role_id")
    private String roleId;

    @Schema(description = "权限ID")
    @TableField("permission_id")
    private String permissionId;

}