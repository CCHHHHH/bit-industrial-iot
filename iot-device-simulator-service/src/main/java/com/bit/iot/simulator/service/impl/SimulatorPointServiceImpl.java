package com.bit.iot.simulator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.simulator.dao.SimulatorPointMapper;
import com.bit.iot.simulator.model.entity.SimulatorPoint;
import com.bit.iot.simulator.service.ISimulatorPointService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
/**
 * 模拟测点服务实现。
 */
public class SimulatorPointServiceImpl extends ServiceImpl<SimulatorPointMapper, SimulatorPoint>
        implements ISimulatorPointService {

    /**
     * 按设备主键查询测点列表。
     *
     * @param deviceId 设备主键
     * @return 测点列表
     */
    @Override
    public List<SimulatorPoint> getPointsByDeviceId(String deviceId) {
        return this.list(new QueryWrapper<SimulatorPoint>()
                .eq("device_id", deviceId)
                .orderByAsc("point_code"));
    }

    /**
     * 新增测点。
     *
     * @param point 测点实体
     * @return 是否成功
     */
    @Override
    public boolean addPoint(SimulatorPoint point) {
        Date now = new Date();
        point.setCreateTime(now);
        point.setUpdateTime(now);
        return this.save(point);
    }

    /**
     * 修改测点。
     *
     * @param point 测点实体
     * @return 是否成功
     */
    @Override
    public boolean editPoint(SimulatorPoint point) {
        point.setUpdateTime(new Date());
        return this.updateById(point);
    }

    /**
     * 删除测点。
     *
     * @param id 测点主键
     * @return 是否成功
     */
    @Override
    public boolean deletePoint(String id) {
        return this.removeById(id);
    }
}
