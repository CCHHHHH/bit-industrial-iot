package com.bit.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.DeviceStationCode;
import com.bit.iot.device.dao.DeviceStationCodeMapper;
import com.bit.iot.device.service.IDeviceStationCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 设备测点表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Service
public class DeviceStationCodeServiceImpl extends ServiceImpl<DeviceStationCodeMapper, DeviceStationCode> implements IDeviceStationCodeService {

    @Override
    public Page<DeviceStationCode> getStationCodeList(Page<DeviceStationCode> page, String deviceId) {
        QueryWrapper<DeviceStationCode> queryWrapper = new QueryWrapper<>();
        if (deviceId != null && !deviceId.isEmpty()) {
            queryWrapper.eq("device_id", deviceId);
        }
        queryWrapper.orderByDesc("id");
        return this.page(page, queryWrapper);
    }
    
    @Override
    public boolean addStationCode(DeviceStationCode stationCode) {
        return this.save(stationCode);
    }
    
    @Override
    public boolean editStationCode(DeviceStationCode stationCode) {
        return this.updateById(stationCode);
    }
    
    @Override
    public boolean deleteStationCode(String id) {
        return this.removeById(id);
    }
    
    @Override
    public List<DeviceStationCode> getStationCodesByDeviceId(String deviceId) {
        QueryWrapper<DeviceStationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_id", deviceId);
        queryWrapper.orderByDesc("id");
        return this.list(queryWrapper);
    }

}
