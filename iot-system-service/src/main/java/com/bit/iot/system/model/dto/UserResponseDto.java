package com.bit.iot.system.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户信息响应 DTO (不包含密码)
 * 
 * @author chenhao
 * @date 2026/3/24
 */
@Getter
@Setter
@Schema(description = "用户信息响应 (不包含密码)")
public class UserResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "中文姓名")
    private String nameCn;

    @Schema(description = "电话号码")
    private String phoneNumber;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态（0-禁用，1-启用）")
    private Integer status;

    @Schema(description = "创建日期")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;

    @Schema(description = "角色列表")
    private List<RoleDto> roles;
}
