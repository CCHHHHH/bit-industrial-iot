package com.bit.iot.simulator.model.vo;

import lombok.Data;

import java.util.Map;

@Data
/**
 * Modbus 寄存器快照视图对象。
 */
public class ModbusRegisterSnapshotVO {
    /**
     * 设备主键。
     */
    private String deviceId;
    /**
     * 设备编码。
     */
    private String deviceCode;
    /**
     * Holding Register 快照。
     */
    private Map<Integer, Integer> holdingRegisters;
}
