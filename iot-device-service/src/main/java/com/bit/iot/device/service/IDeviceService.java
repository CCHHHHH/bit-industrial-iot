package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.device.model.entity.Device;

import java.util.List;

/**
 * <p>
 * 设备表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
public interface IDeviceService extends IService<Device> {
    
    /**
     * 分页查询设备列表
     * @param page 分页信息
     * @param deviceName 设备名称
     * @param catalogueId 设备目录 ID
     * @return 设备列表
     */
    Page<Device> getDeviceList(Page<Device> page, String deviceName, String catalogueId);
    
    /**
     * 新增设备
     * @param device 设备信息
     * @return 是否成功
     */
    boolean addDevice(Device device);
    
    /**
     * 编辑设备
     * @param device 设备信息
     * @return 是否成功
     */
    boolean editDevice(Device device);
    
    /**
     * 删除设备
     * @param id 设备 ID
     * @return 是否成功
     */
    boolean deleteDevice(String id);
    
    /**
     * 根据设备目录 ID 查询设备列表
     * @param catalogueId 设备目录 ID
     * @return 设备列表
     */
    List<Device> getDevicesByCatalogueId(String catalogueId);
    
}
