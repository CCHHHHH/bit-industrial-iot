package com.bit.iot.simulator.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.bit.iot.simulator.model.vo.ModbusRegisterSnapshotVO;
import com.bit.iot.simulator.scheduler.ModbusRegisterStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulator/modbus")
@Tag(name = "设备模拟-Modbus寄存器快照")
/**
 * Modbus 寄存器快照查询控制器。
 */
public class SimulatorModbusController extends BaseController {

    /**
     * Modbus 寄存器缓存。
     */
    private final ModbusRegisterStore registerStore;

    /**
     * 构造 Modbus 寄存器快照查询控制器。
     *
     * @param registerStore Modbus 寄存器缓存
     */
    public SimulatorModbusController(ModbusRegisterStore registerStore) {
        this.registerStore = registerStore;
    }

    /**
     * 查询设备 Modbus 寄存器快照。
     *
     * @param deviceId 设备主键
     * @return 寄存器快照
     */
    @GetMapping("/{deviceId}/registers")
    @Operation(summary = "查询设备 Modbus 寄存器快照")
    public Result<ModbusRegisterSnapshotVO> registers(@PathVariable String deviceId) {
        ModbusRegisterStore.Snapshot snapshot = registerStore.getByDeviceId(deviceId);
        if (snapshot == null) {
            return error("未找到寄存器快照");
        }
        ModbusRegisterSnapshotVO vo = new ModbusRegisterSnapshotVO();
        vo.setDeviceId(snapshot.getDeviceId());
        vo.setDeviceCode(snapshot.getDeviceCode());
        vo.setHoldingRegisters(snapshot.getHoldingRegisters());
        return success(vo);
    }
}
