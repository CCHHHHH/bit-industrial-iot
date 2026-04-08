package com.bit.iot.simulator.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
/**
 * 模拟测点新增和修改请求。
 */
public class SimulatorPointRequest {
    /**
     * 测点主键。
     */
    private String id;
    /**
     * 关联设备主键。
     */
    @NotBlank
    private String deviceId;
    /**
     * 测点编码。
     */
    @NotBlank
    private String pointCode;
    /**
     * 测点名称。
     */
    @NotBlank
    private String pointName;
    /**
     * 计量单位。
     */
    private String unit;
    /**
     * 最小值。
     */
    @NotNull
    private Double minValue;
    /**
     * 最大值。
     */
    @NotNull
    private Double maxValue;
    /**
     * 保留小数位数。
     */
    private Integer precisionScale = 2;
    /**
     * 数据质量码。
     */
    private Integer quality = 0;
    /**
     * Modbus 寄存器地址。
     */
    private Integer registerAddress;
    /**
     * Modbus 寄存器类型。
     */
    private String registerType;
}
