package com.bit.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.Device;
import com.bit.iot.device.dao.DeviceMapper;
import com.bit.iot.device.service.IDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 设备表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService {

    @Override
    public Page<Device> getDeviceList(Page<Device> page, String deviceName, String catalogueId) {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        if (deviceName != null && !deviceName.isEmpty()) {
            queryWrapper.like("device_name", deviceName);
        }
        if (catalogueId != null && !catalogueId.isEmpty()) {
            queryWrapper.eq("catalogue_id", catalogueId);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }
    
    @Override
    public boolean addDevice(Device device) {
        Date now = new Date();
        device.setCreateTime(now);
        device.setUpdateTime(now);
        return this.save(device);
    }
    
    @Override
    public boolean editDevice(Device device) {
        Date now = new Date();
        device.setUpdateTime(now);
        return this.updateById(device);
    }
    
    @Override
    public boolean deleteDevice(String id) {
        return this.removeById(id);
    }
    
    @Override
    public List<Device> getDevicesByCatalogueId(String catalogueId) {
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("catalogue_id", catalogueId);
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

}
