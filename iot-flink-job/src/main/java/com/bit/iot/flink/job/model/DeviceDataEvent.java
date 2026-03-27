package com.bit.iot.flink.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备时序数据事件（Flink 流中的数据元素）
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDataEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备 ID */
    private String deviceId;

    /** 测点编码 */
    private String pointCode;

    /** 事件时间戳（毫秒） */
    private long timestamp;

    /** 数值 */
    private double value;

    /** 质量码 */
    private int quality;
}
