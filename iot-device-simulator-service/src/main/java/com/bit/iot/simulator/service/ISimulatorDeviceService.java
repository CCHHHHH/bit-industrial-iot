package com.bit.iot.simulator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.simulator.model.entity.SimulatorDevice;

/**
 * 模拟设备服务接口。
 */
public interface ISimulatorDeviceService extends IService<SimulatorDevice> {
    /**
     * 分页查询模拟设备。
     *
     * @param page 分页参数
     * @param keyword 关键字
     * @param deviceType 设备类型
     * @param deviceStatus 设备状态
     * @return 分页结果
     */
    Page<SimulatorDevice> getDevicePage(Page<SimulatorDevice> page, String keyword, String deviceType, String deviceStatus);

    /**
     * 新增模拟设备。
     *
     * @param device 设备实体
     * @return 是否成功
     */
    boolean addDevice(SimulatorDevice device);

    /**
     * 修改模拟设备。
     *
     * @param device 设备实体
     * @return 是否成功
     */
    boolean editDevice(SimulatorDevice device);

    /**
     * 删除模拟设备。
     *
     * @param id 设备主键
     * @return 是否成功
     */
    boolean deleteDevice(String id);
}
