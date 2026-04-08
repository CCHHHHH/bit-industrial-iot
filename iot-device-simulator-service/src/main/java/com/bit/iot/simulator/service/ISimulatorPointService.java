package com.bit.iot.simulator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.simulator.model.entity.SimulatorPoint;

import java.util.List;

/**
 * 模拟测点服务接口。
 */
public interface ISimulatorPointService extends IService<SimulatorPoint> {
    /**
     * 按设备主键查询测点列表。
     *
     * @param deviceId 设备主键
     * @return 测点列表
     */
    List<SimulatorPoint> getPointsByDeviceId(String deviceId);

    /**
     * 新增测点。
     *
     * @param point 测点实体
     * @return 是否成功
     */
    boolean addPoint(SimulatorPoint point);

    /**
     * 修改测点。
     *
     * @param point 测点实体
     * @return 是否成功
     */
    boolean editPoint(SimulatorPoint point);

    /**
     * 删除测点。
     *
     * @param id 测点主键
     * @return 是否成功
     */
    boolean deletePoint(String id);
}
