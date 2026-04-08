package com.bit.iot.simulator.service.protocol.impl;

import com.bit.iot.simulator.model.entity.SimulatorTask;
import com.bit.iot.simulator.model.enums.ProtocolTypeEnum;
import com.bit.iot.simulator.service.protocol.ProtocolPayload;
import com.bit.iot.simulator.service.protocol.ProtocolSendResult;
import com.bit.iot.simulator.service.protocol.ProtocolSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
/**
 * HTTP 协议发送器。
 */
public class HttpProtocolSender implements ProtocolSender {

    /**
     * HTTP 客户端。
     */
    private final HttpClient httpClient;
    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造 HTTP 协议发送器。
     *
     * @param httpClient HTTP 客户端
     */
    public HttpProtocolSender(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 返回支持的协议类型。
     *
     * @return HTTP 协议
     */
    @Override
    public ProtocolTypeEnum protocolType() {
        return ProtocolTypeEnum.HTTP;
    }

    /**
     * 通过 HTTP 请求发送模拟数据。
     *
     * @param payload 协议发送上下文
     * @return 发送结果
     * @throws Exception 发送异常
     */
    @Override
    public ProtocolSendResult send(ProtocolPayload payload) throws Exception {
        SimulatorTask task = payload.getTask();
        String body = objectMapper.writeValueAsString(payload.getPayload());
        // 默认使用 POST，避免空方法配置导致请求构造失败。
        String method = task.getHttpMethod() == null || task.getHttpMethod().isBlank()
                ? "POST" : task.getHttpMethod().toUpperCase();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(task.getHttpUrl()))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // 统一按 2xx 作为成功标准，其余状态码带回响应体便于定位问题。
        return response.statusCode() >= 200 && response.statusCode() < 300
                ? ProtocolSendResult.success("HTTP " + response.statusCode())
                : ProtocolSendResult.failure("HTTP " + response.statusCode() + ": " + response.body());
    }
}
