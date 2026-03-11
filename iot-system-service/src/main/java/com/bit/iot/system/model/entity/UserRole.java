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
 * 用户角色关联表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-09
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("user_role")
@Schema(description = "用户角色关联实体")
public class UserRole {

    @TableId("id")
    private String id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private String userId;

    @Schema(description = "角色ID")
    @TableField("role_id")
    private String roleId;

}