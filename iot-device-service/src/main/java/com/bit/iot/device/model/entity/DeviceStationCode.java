package com.bit.iot.device.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
/**
 * <p>
 * 设备测点表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("device_station_code")
@Schema(description = "设备测点实体")
public class DeviceStationCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 设备 id
     */
    @TableField("device_id")
    @Schema(description = "设备 id")
    private String deviceId;
    
    /**
     * 测点原始编码
     */
    @Schema(description = "测点原始编码")
    @TableField("station_code")
    private String stationCode;
    
    /**
     * 测点名称
     */
    @Schema(description = "测点名称")
    @TableField("station_name")
    private String stationName;
    
    /**
     * 测点标准编码
     */
    @Schema(description = "测点标准编码")
    @TableField("standard_station_code")
    private String standardStationCode;
    
    /**
     * 测点描述
     */
    @Schema(description = "测点描述")
    @TableField("station_desc")
    private String stationDesc;
}
