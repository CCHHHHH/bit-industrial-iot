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
 * 集成实例配置表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@Getter
@Setter
@ToString
@TableName("integration_config")
@Accessors(chain = true)
@Schema(description = "集成实例配置表")
public class IntegrationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 集成实例名称
     */
    @Schema(description = "集成实例名称")
    @TableField("integration_name")
    private String integrationName;
    
    /**
     * 插件 id
     */
    @Schema(description = "插件 id")
    @TableField("plugin_id")
    private String pluginId;

    /**
     * 集成状态（0-停用，1-运行中）
     */
    @Schema(description = "集成状态")
    @TableField("integration_status")
    private Integer integrationStatus;

    /**
     * 集成描述
     */
    @Schema(description = "集成描述")
    @TableField("integration_desc")
    private String integrationDesc;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Date updateTime;
}
