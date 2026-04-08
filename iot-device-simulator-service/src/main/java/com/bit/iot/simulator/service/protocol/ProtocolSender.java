package com.bit.iot.simulator.service.protocol;

import com.bit.iot.simulator.model.enums.ProtocolTypeEnum;

/**
 * 协议发送器接口。
 */
public interface ProtocolSender {
    /**
     * 返回当前发送器支持的协议类型。
     *
     * @return 协议类型
     */
    ProtocolTypeEnum protocolType();

    /**
     * 按指定协议发送模拟数据。
     *
     * @param payload 协议发送上下文
     * @return 发送结果
     * @throws Exception 发送异常
     */
    ProtocolSendResult send(ProtocolPayload payload) throws Exception;
}
