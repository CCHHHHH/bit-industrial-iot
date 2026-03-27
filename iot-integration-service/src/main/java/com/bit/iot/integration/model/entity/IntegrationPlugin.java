package com.bit.iot.integration.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
/**
 * <p>
 * 集成管理插件表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@Getter
@Setter
@ToString
@TableName("integration_plugin")
@Accessors(chain = true)
@Schema(description = "集成管理插件表")
public class IntegrationPlugin implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 插件名称
     */
    @Schema(description = "插件名称")
    @TableField("plugin_name")
    private String pluginName;

    /**
     * 插件描述
     */
    @Schema(description = "插件描述")
    @TableField("plugin_description")
    private String pluginDescription;

    /**
     * 插件路径
     */
    @Schema(description = "插件路径")
    @TableField("plugin_path")
    private String pluginPath;

    /**
     * 插件类型
     */
    @Schema(description = "插件类型")
    @TableField("plugin_type")
    private String pluginType;

    /**
     * 插件状态（0-禁用，1-启用）
     */
    @Schema(description = "插件状态")
    @TableField("plugin_status")
    private Integer pluginStatus;

    /**
     * 插件版本
     */
    @Schema(description = "插件版本")
    @TableField("plugin_version")
    private String pluginVersion;

    /**
     * 插件大小（字节）
     */
    @Schema(description = "插件大小")
    @TableField("plugin_size")
    private Integer pluginSize;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Date updateTime;
}
