package com.bit.iot.device.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
/**
 * <p>
 * 设备事件表
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("device_event")
@Schema(description = "设备事件实体")
public class DeviceEvent implements Serializable {

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
     * 事件日期
     */
    @Schema(description = "事件日期")
    @TableField("event_time")
    private Date eventTime;
    
    /**
     * 事件内容
     */
    @Schema(description = "事件内容")
    @TableField("event_message")
    private String eventMessage;
}
