package com.bit.iot.device.model.entity;

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
 * 设备表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Getter
@Setter
@ToString
@TableName("device")
@Accessors(chain = true)
@Schema(description = "设备表")
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 设备名称
     */
    @Schema(description = "设备名称")
    @TableField("device_name")
    private String deviceName;

    /**
     * 设备编码
     */
    @Schema(description = "设备编码")
    @TableField("device_code")
    private String deviceCode;

    /**
     * 设备状态
     */
    @TableField("status")
    @Schema(description = "设备状态")
    private String status;

    /**
     * 设备类型
     */
    @Schema(description = "设备类型")
    @TableField("device_type")
    private String deviceType;

    /**
     * 目录id
     */
    @Schema(description = "目录id")
    @TableField("catalogue_id")
    private String catalogueId;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
