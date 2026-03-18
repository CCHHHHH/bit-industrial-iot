package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.device.model.entity.DeviceStationCode;

import java.util.List;

/**
 * <p>
 * 设备测点表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
public interface IDeviceStationCodeService extends IService<DeviceStationCode> {
    
    /**
     * 分页查询设备测点列表
     * @param page 分页信息
     * @param deviceId 设备 ID
     * @return 设备测点列表
     */
    Page<DeviceStationCode> getStationCodeList(Page<DeviceStationCode> page, String deviceId);
    
    /**
     * 新增设备测点
     * @param stationCode 设备测点信息
     * @return 是否成功
     */
    boolean addStationCode(DeviceStationCode stationCode);
    
    /**
     * 编辑设备测点
     * @param stationCode 设备测点信息
     * @return 是否成功
     */
    boolean editStationCode(DeviceStationCode stationCode);
    
    /**
     * 删除设备测点
     * @param id 测点 ID
     * @return 是否成功
     */
    boolean deleteStationCode(String id);
    
    /**
     * 根据设备 ID 查询测点列表
     * @param deviceId 设备 ID
     * @return 设备测点列表
     */
    List<DeviceStationCode> getStationCodesByDeviceId(String deviceId);
    
}
