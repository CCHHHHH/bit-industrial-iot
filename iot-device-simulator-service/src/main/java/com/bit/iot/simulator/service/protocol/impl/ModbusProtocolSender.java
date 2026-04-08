package com.bit.iot.simulator.service.protocol.impl;

import com.bit.iot.simulator.model.entity.SimulatorPoint;
import com.bit.iot.simulator.model.enums.ProtocolTypeEnum;
import com.bit.iot.simulator.scheduler.ModbusRegisterStore;
import com.bit.iot.simulator.service.protocol.ProtocolPayload;
import com.bit.iot.simulator.service.protocol.ProtocolSendResult;
import com.bit.iot.simulator.service.protocol.ProtocolSender;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
/**
 * Modbus TCP 协议发送器。
 */
public class ModbusProtocolSender implements ProtocolSender {

    /**
     * Modbus 寄存器缓存。
     */
    private final ModbusRegisterStore registerStore;

    /**
     * 构造 Modbus TCP 协议发送器。
     *
     * @param registerStore Modbus 寄存器缓存
     */
    public ModbusProtocolSender(ModbusRegisterStore registerStore) {
        this.registerStore = registerStore;
    }

    /**
     * 返回支持的协议类型。
     *
     * @return Modbus TCP 协议
     */
    @Override
    public ProtocolTypeEnum protocolType() {
        return ProtocolTypeEnum.MODBUS_TCP;
    }

    /**
     * 将测点值映射为寄存器快照并更新缓存。
     *
     * @param payload 协议发送上下文
     * @return 发送结果
     */
    @Override
    public ProtocolSendResult send(ProtocolPayload payload) {
        Map<Integer, Integer> registers = new HashMap<>();
        for (SimulatorPoint point : payload.getPoints()) {
            Object value = ((Map<?, ?>) payload.getPayload().get("values")).get(point.getPointCode());
            if (point.getRegisterAddress() != null && value instanceof Number number) {
                // 当前实现按整数寄存器快照存储，小数值先四舍五入后写入。
                registers.put(point.getRegisterAddress(), (int) Math.round(number.doubleValue()));
            }
        }
        // 本地缓存快照用于调试和读取，不直接建立真实 Modbus 连接。
        registerStore.update(payload.getDevice(), registers);
        return ProtocolSendResult.success("Modbus register snapshot updated");
    }
}
