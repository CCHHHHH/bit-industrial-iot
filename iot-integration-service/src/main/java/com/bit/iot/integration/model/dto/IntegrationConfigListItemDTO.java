package com.bit.iot.integration.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 集成配置列表项 DTO（平铺字段，包含插件名称）
 *
 * @author chenhao
 * @date 2026/3/20
 */
@Getter
@Setter
@Schema(description = "集成配置列表项")
public class IntegrationConfigListItemDTO {

    @Schema(description = "集成实例 ID")
    private String id;

    @Schema(description = "集成实例名称")
    private String integrationName;

    @Schema(description = "插件 ID")
    private String pluginId;

    @Schema(description = "插件名称")
    private String pluginName;

    @Schema(description = "集成状态（0-停用，1-运行中）")
    private Integer integrationStatus;

    @Schema(description = "集成描述")
    private String integrationDesc;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
