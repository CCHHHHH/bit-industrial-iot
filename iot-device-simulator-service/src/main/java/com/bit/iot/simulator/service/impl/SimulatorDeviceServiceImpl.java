package com.bit.iot.simulator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.simulator.dao.SimulatorDeviceMapper;
import com.bit.iot.simulator.model.entity.SimulatorDevice;
import com.bit.iot.simulator.service.ISimulatorDeviceService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
/**
 * 模拟设备服务实现。
 */
public class SimulatorDeviceServiceImpl extends ServiceImpl<SimulatorDeviceMapper, SimulatorDevice>
        implements ISimulatorDeviceService {

    /**
     * 分页查询模拟设备。
     *
     * @param page 分页参数
     * @param keyword 关键字
     * @param deviceType 设备类型
     * @param deviceStatus 设备状态
     * @return 分页结果
     */
    @Override
    public Page<SimulatorDevice> getDevicePage(Page<SimulatorDevice> page, String keyword, String deviceType, String deviceStatus) {
        QueryWrapper<SimulatorDevice> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like("device_name", keyword).or().like("device_code", keyword));
        }
        if (deviceType != null && !deviceType.isBlank()) {
            queryWrapper.eq("device_type", deviceType);
        }
        if (deviceStatus != null && !deviceStatus.isBlank()) {
            queryWrapper.eq("device_status", deviceStatus);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }

    /**
     * 新增模拟设备。
     *
     * @param device 设备实体
     * @return 是否成功
     */
    @Override
    public boolean addDevice(SimulatorDevice device) {
        Date now = new Date();
        device.setCreateTime(now);
        device.setUpdateTime(now);
        return this.save(device);
    }

    /**
     * 修改模拟设备。
     *
     * @param device 设备实体
     * @return 是否成功
     */
    @Override
    public boolean editDevice(SimulatorDevice device) {
        device.setUpdateTime(new Date());
        return this.updateById(device);
    }

    /**
     * 删除模拟设备。
     *
     * @param id 设备主键
     * @return 是否成功
     */
    @Override
    public boolean deleteDevice(String id) {
        return this.removeById(id);
    }
}
