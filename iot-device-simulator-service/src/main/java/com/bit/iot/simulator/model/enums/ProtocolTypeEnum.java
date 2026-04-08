package com.bit.iot.simulator.model.enums;

/**
 * 模拟任务支持的协议类型枚举。
 */
public enum ProtocolTypeEnum {
    /**
     * HTTP 协议。
     */
    HTTP,
    /**
     * MQTT 协议。
     */
    MQTT,
    /**
     * Kafka 协议。
     */
    KAFKA,
    /**
     * Modbus TCP 协议。
     */
    MODBUS_TCP;

    /**
     * 安全解析协议类型，空值默认返回 HTTP。
     *
     * @param value 协议类型字符串
     * @return 解析后的协议类型
     */
    public static ProtocolTypeEnum safeValueOf(String value) {
        if (value == null || value.isBlank()) {
            return HTTP;
        }
        return ProtocolTypeEnum.valueOf(value.trim().toUpperCase());
    }
}
