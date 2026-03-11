package com.bit.iot.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 权限表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("permission")
@Schema(description = "权限实体")
public class Permission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @Schema(description = "权限名")
    @TableField("permission_name")
    private String permissionName;

    @Schema(description = "权限代码")
    @TableField("permission_code")
    private String permissionCode;

    @Schema(description = "权限描述")
    @TableField("permission_desc")
    private String permissionDesc;

    @Schema(description = "权限类型")
    @TableField("permission_type")
    private String permissionType;
}
