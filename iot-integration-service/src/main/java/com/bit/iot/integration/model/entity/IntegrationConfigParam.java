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
/**
 * <p>
 * 集成实例配置参数表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-20 11:32:38
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("integration_config_param")
@Schema(description = "集成实例配置参数表")
public class IntegrationConfigParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 集成实例id
     */
    @Schema(description = "集成实例id")
    @TableField("integration_id")
    private String integrationId;

    /**
     * 插件配置参数key
     */
    @TableField("param_key")
    @Schema(description = "插件配置参数key")
    private String paramKey;

    /**
     * 插件配置参数value
     */
    @TableField("param_value")
    @Schema(description = "插件配置参数value")
    private String paramValue;

    /**
     * 插件配置参数描述
     */
    @TableField("param_desc")
    @Schema(description = "插件配置参数描述")
    private String paramDesc;
}
