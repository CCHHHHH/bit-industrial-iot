package com.bit.iot.common.flink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 时序数据点
 * 从 TDEngine 读取后封装成此结构传入算法
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备 ID */
    private String deviceId;

    /** 测点编码 */
    private String pointCode;

    /** 数据时间戳 */
    private Date timestamp;

    /** 数值 */
    private Double value;

    /** 质量码（0-好，1-坏，2-不确定） */
    private Integer quality;
}
