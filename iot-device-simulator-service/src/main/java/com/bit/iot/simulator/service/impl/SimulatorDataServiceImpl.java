package com.bit.iot.simulator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.simulator.dao.SimulatorSendLogMapper;
import com.bit.iot.simulator.dao.SimulatorTimeseriesDataMapper;
import com.bit.iot.simulator.model.entity.SimulatorSendLog;
import com.bit.iot.simulator.model.entity.SimulatorTimeseriesData;
import com.bit.iot.simulator.service.ISimulatorDataService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 模拟数据查询服务实现。
 */
public class SimulatorDataServiceImpl implements ISimulatorDataService {

    /**
     * 时序数据 Mapper。
     */
    private final SimulatorTimeseriesDataMapper timeseriesDataMapper;
    /**
     * 发送日志 Mapper。
     */
    private final SimulatorSendLogMapper sendLogMapper;

    /**
     * 构造模拟数据查询服务实现。
     *
     * @param timeseriesDataMapper 时序数据 Mapper
     * @param sendLogMapper 发送日志 Mapper
     */
    public SimulatorDataServiceImpl(SimulatorTimeseriesDataMapper timeseriesDataMapper,
                                    SimulatorSendLogMapper sendLogMapper) {
        this.timeseriesDataMapper = timeseriesDataMapper;
        this.sendLogMapper = sendLogMapper;
    }

    /**
     * 分页查询历史时序数据。
     *
     * @param page 分页参数
     * @param deviceId 设备主键
     * @param pointCode 测点编码
     * @param protocolType 协议类型
     * @return 分页结果
     */
    @Override
    public Page<SimulatorTimeseriesData> getHistory(Page<SimulatorTimeseriesData> page, String deviceId, String pointCode, String protocolType) {
        QueryWrapper<SimulatorTimeseriesData> queryWrapper = new QueryWrapper<>();
        if (deviceId != null && !deviceId.isBlank()) {
            queryWrapper.eq("device_id", deviceId);
        }
        if (pointCode != null && !pointCode.isBlank()) {
            queryWrapper.eq("point_code", pointCode);
        }
        if (protocolType != null && !protocolType.isBlank()) {
            queryWrapper.eq("protocol_type", protocolType);
        }
        queryWrapper.orderByDesc("generated_time");
        return timeseriesDataMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询设备最新测点值。
     *
     * @param deviceId 设备主键
     * @return 最新测点数据列表
     */
    @Override
    public List<SimulatorTimeseriesData> getLatestByDeviceId(String deviceId) {
        List<SimulatorTimeseriesData> rawList = timeseriesDataMapper.selectList(new QueryWrapper<SimulatorTimeseriesData>()
                .eq("device_id", deviceId)
                .orderByDesc("generated_time"));
        Map<String, SimulatorTimeseriesData> latestMap = new LinkedHashMap<>();
        for (SimulatorTimeseriesData item : rawList) {
            // 查询结果已按时间倒序排列，首次写入即为每个测点的最新值。
            latestMap.putIfAbsent(item.getPointCode(), item);
        }
        return new ArrayList<>(latestMap.values());
    }

    /**
     * 分页查询发送日志。
     *
     * @param page 分页参数
     * @param taskId 任务主键
     * @param protocolType 协议类型
     * @param sendStatus 发送状态
     * @return 分页结果
     */
    @Override
    public Page<SimulatorSendLog> getSendLogPage(Page<SimulatorSendLog> page, String taskId, String protocolType, String sendStatus) {
        QueryWrapper<SimulatorSendLog> queryWrapper = new QueryWrapper<>();
        if (taskId != null && !taskId.isBlank()) {
            queryWrapper.eq("task_id", taskId);
        }
        if (protocolType != null && !protocolType.isBlank()) {
            queryWrapper.eq("protocol_type", protocolType);
        }
        if (sendStatus != null && !sendStatus.isBlank()) {
            queryWrapper.eq("send_status", sendStatus);
        }
        queryWrapper.orderByDesc("sent_time");
        return sendLogMapper.selectPage(page, queryWrapper);
    }
}
