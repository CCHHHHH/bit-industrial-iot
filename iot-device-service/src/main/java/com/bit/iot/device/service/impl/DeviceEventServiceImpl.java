package com.bit.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.DeviceEvent;
import com.bit.iot.device.dao.DeviceEventMapper;
import com.bit.iot.device.service.IDeviceEventService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 设备事件表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Service
public class DeviceEventServiceImpl extends ServiceImpl<DeviceEventMapper, DeviceEvent> implements IDeviceEventService {

    @Override
    public Page<DeviceEvent> getEventList(Page<DeviceEvent> page, String deviceId) {
        QueryWrapper<DeviceEvent> queryWrapper = new QueryWrapper<>();
        if (deviceId != null && !deviceId.isEmpty()) {
            queryWrapper.eq("device_id", deviceId);
        }
        queryWrapper.orderByDesc("event_time");
        return this.page(page, queryWrapper);
    }
    
    @Override
    public boolean addEvent(DeviceEvent event) {
        Date now = new Date();
        event.setEventTime(now);
        return this.save(event);
    }
    
    @Override
    public boolean editEvent(DeviceEvent event) {
        return this.updateById(event);
    }
    
    @Override
    public boolean deleteEvent(String id) {
        return this.removeById(id);
    }
    
    @Override
    public List<DeviceEvent> getEventsByDeviceId(String deviceId) {
        QueryWrapper<DeviceEvent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_id", deviceId);
        queryWrapper.orderByDesc("event_time");
        return this.list(queryWrapper);
    }

}
