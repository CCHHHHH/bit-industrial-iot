package com.bit.iot.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-04 03:07:02
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("user")
@Schema(description = "用户实体")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId("id")
    private String id;

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "密文密码")
    @TableField("password")
    private String password;

    @Schema(description = "电话号码")
    @TableField("phone_number")
    private String phoneNumber;

    @Schema(description = "创建日期")
    @TableField("create_time")
    private Date createTime;

    @Schema(description = "修改时间")
    @TableField("update_time")
    private Date updateTime;
}
