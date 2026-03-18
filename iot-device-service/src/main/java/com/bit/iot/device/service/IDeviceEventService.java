package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.device.model.entity.DeviceEvent;

import java.util.List;

/**
 * <p>
 * 设备事件表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
public interface IDeviceEventService extends IService<DeviceEvent> {
    
    /**
     * 分页查询设备事件列表
     * @param page 分页信息
     * @param deviceId 设备 ID
     * @return 设备事件列表
     */
    Page<DeviceEvent> getEventList(Page<DeviceEvent> page, String deviceId);
    
    /**
     * 新增设备事件
     * @param event 设备事件信息
     * @return 是否成功
     */
    boolean addEvent(DeviceEvent event);
    
    /**
     * 编辑设备事件
     * @param event 设备事件信息
     * @return 是否成功
     */
    boolean editEvent(DeviceEvent event);
    
    /**
     * 删除设备事件
     * @param id 事件 ID
     * @return 是否成功
     */
    boolean deleteEvent(String id);
    
    /**
     * 根据设备 ID 查询事件列表
     * @param deviceId 设备 ID
     * @return 设备事件列表
     */
    List<DeviceEvent> getEventsByDeviceId(String deviceId);
    
}
