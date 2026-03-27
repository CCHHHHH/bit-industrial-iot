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
 * 集成实例数据映射表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@Getter
@Setter
@ToString
@TableName("integration_data_mapping")
@Accessors(chain = true)
@Schema(description = "集成实例数据映射表")
public class IntegrationDataMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 集成配置 id
     */
    @Schema(description = "集成配置 id")
    @TableField("integration_id")
    private String integrationId;
    
    /**
     * 原始数据
     */
    @Schema(description = "JSON 数据")
    @TableField("source_data")
    private String sourceData;

    /**
     * 映射类型
     */
    @Schema(description = "映射类型，（设备属性、设备状态、时序数据）")
    @TableField("mapping_type")
    private String mappingType;

    /**
     * 调度周期
     */
    @Schema(description = "调度周期")
    @TableField("scheduler_time")
    private Long schedulerTime;

    /**
     * 调度单位
     */
    @Schema(description = "调度单位，（d、h、m、s）")
    @TableField("scheduler_unit")
    private String schedulerUnit;

}
