package com.bit.iot.simulator.scheduler;

import com.bit.iot.simulator.model.entity.SimulatorDevice;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
/**
 * Modbus 寄存器快照缓存。
 */
public class ModbusRegisterStore {

    @Getter
    /**
     * Modbus 寄存器快照。
     */
    public static class Snapshot {
        /**
         * 设备主键。
         */
        private final String deviceId;
        /**
         * 设备编码。
         */
        private final String deviceCode;
        /**
         * Holding Register 内容。
         */
        private final Map<Integer, Integer> holdingRegisters;

        /**
         * 构造寄存器快照。
         *
         * @param deviceId 设备主键
         * @param deviceCode 设备编码
         * @param holdingRegisters Holding Register 内容
         */
        public Snapshot(String deviceId, String deviceCode, Map<Integer, Integer> holdingRegisters) {
            this.deviceId = deviceId;
            this.deviceCode = deviceCode;
            this.holdingRegisters = holdingRegisters;
        }
    }

    /**
     * 设备寄存器快照缓存。
     */
    private final Map<String, Snapshot> snapshotMap = new ConcurrentHashMap<>();

    /**
     * 更新设备寄存器快照。
     *
     * @param device 设备实体
     * @param registers 寄存器值
     */
    public void update(SimulatorDevice device, Map<Integer, Integer> registers) {
        snapshotMap.put(device.getId(), new Snapshot(
                device.getId(),
                device.getDeviceCode(),
                new ConcurrentHashMap<>(registers)));
    }

    /**
     * 按设备主键查询寄存器快照。
     *
     * @param deviceId 设备主键
     * @return 寄存器快照
     */
    public Snapshot getByDeviceId(String deviceId) {
        return snapshotMap.get(deviceId);
    }

    /**
     * 获取全部寄存器快照。
     *
     * @return 不可变快照映射
     */
    public Map<String, Snapshot> all() {
        return Collections.unmodifiableMap(snapshotMap);
    }
}
