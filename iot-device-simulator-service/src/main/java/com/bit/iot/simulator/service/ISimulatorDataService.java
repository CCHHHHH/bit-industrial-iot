package com.bit.iot.simulator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.simulator.model.entity.SimulatorSendLog;
import com.bit.iot.simulator.model.entity.SimulatorTimeseriesData;

import java.util.List;

/**
 * 模拟数据查询服务接口。
 */
public interface ISimulatorDataService {
    /**
     * 分页查询历史时序数据。
     *
     * @param page 分页参数
     * @param deviceId 设备主键
     * @param pointCode 测点编码
     * @param protocolType 协议类型
     * @return 分页结果
     */
    Page<SimulatorTimeseriesData> getHistory(Page<SimulatorTimeseriesData> page, String deviceId, String pointCode, String protocolType);

    /**
     * 查询设备最新测点值。
     *
     * @param deviceId 设备主键
     * @return 最新测点数据列表
     */
    List<SimulatorTimeseriesData> getLatestByDeviceId(String deviceId);

    /**
     * 分页查询发送日志。
     *
     * @param page 分页参数
     * @param taskId 任务主键
     * @param protocolType 协议类型
     * @param sendStatus 发送状态
     * @return 分页结果
     */
    Page<SimulatorSendLog> getSendLogPage(Page<SimulatorSendLog> page, String taskId, String protocolType, String sendStatus);
}
