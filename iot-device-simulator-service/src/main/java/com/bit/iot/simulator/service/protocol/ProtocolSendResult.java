package com.bit.iot.simulator.service.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
/**
 * 协议发送结果。
 */
public class ProtocolSendResult {
    /**
     * 是否发送成功。
     */
    private boolean success;
    /**
     * 发送结果说明。
     */
    private String message;

    /**
     * 创建成功结果。
     *
     * @param message 成功说明
     * @return 成功结果对象
     */
    public static ProtocolSendResult success(String message) {
        return new ProtocolSendResult(true, message);
    }

    /**
     * 创建失败结果。
     *
     * @param message 失败说明
     * @return 失败结果对象
     */
    public static ProtocolSendResult failure(String message) {
        return new ProtocolSendResult(false, message);
    }
}
