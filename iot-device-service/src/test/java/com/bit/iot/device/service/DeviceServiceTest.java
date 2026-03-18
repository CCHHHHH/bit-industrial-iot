package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.Device;
import com.bit.iot.device.model.entity.DeviceCatalogue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备服务测试类
 */
@SpringBootTest
public class DeviceServiceTest {

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceCatalogueService catalogueService;

    /**
     * 测试新增设备
     */
    @Test
    public void testAddDevice() {
        // 先创建一个目录用于关联
        DeviceCatalogue catalogue = new DeviceCatalogue();
        catalogue.setParentId("");
        catalogue.setCatalogueName("设备测试目录");
        catalogueService.addCatalogue(catalogue);

        // 创建设备 1
        Device device1 = new Device();
        device1.setDeviceName("测试设备 1 号");
        device1.setCatalogueId(catalogue.getId());
        device1.setDeviceType("类型 A");
        device1.setDeviceCode("CODE-X-100");
        device1.setStatus("在线");

        boolean result = deviceService.addDevice(device1);
        assertTrue(result, "添加设备失败");
        System.out.println("设备 1 添加成功，ID: " + device1.getId());

        // 创建设备 2
        Device device2 = new Device();
        device2.setDeviceName("测试设备 2 号");
        device2.setCatalogueId(catalogue.getId());
        device2.setDeviceType("类型 B");
        device2.setDeviceCode("CODE-Y-200");
        device2.setStatus("离线");

        result = deviceService.addDevice(device2);
        assertTrue(result, "添加设备失败");
        System.out.println("设备 2 添加成功，ID: " + device2.getId());

        // 创建设备 3（不同目录）
        Device device3 = new Device();
        device3.setDeviceName("测试设备 3 号");
        device3.setCatalogueId(null);
        device3.setDeviceType("类型 A");
        device3.setDeviceCode("CODE-Z-300");
        device3.setStatus("维护中");
        
        result = deviceService.addDevice(device3);
        assertTrue(result, "添加设备失败");
        System.out.println("设备 3 添加成功，ID: " + device3.getId());
    }

    /**
     * 测试分页查询设备列表
     */
    @Test
    public void testGetDeviceList() {
        Page<Device> page = new Page<>(1, 10);
        Page<Device> result = deviceService.getDeviceList(page, null, null);
        
        assertNotNull(result, "查询结果为空");
        System.out.println("总记录数：" + result.getTotal());
        System.out.println("当前页码：" + result.getCurrent());
        System.out.println("每页大小：" + result.getSize());
        
        for (Device device : result.getRecords()) {
            System.out.println("设备 ID: " + device.getId() + 
                             ", 名称：" + device.getDeviceName() + 
                             ", 类型：" + device.getDeviceType() +
                             ", 状态：" + device.getStatus());
        }
    }

    /**
     * 测试按名称查询设备
     */
    @Test
    public void testGetDeviceListByName() {
        Page<Device> page = new Page<>(1, 10);
        Page<Device> result = deviceService.getDeviceList(page, "测试设备", null);
        
        assertNotNull(result, "查询结果为空");
        System.out.println("找到 " + result.getTotal() + " 个包含'测试设备'的设备");
        
        for (Device device : result.getRecords()) {
            System.out.println("设备名称：" + device.getDeviceName() + 
                             ", 编码：" + device.getDeviceCode());
        }
    }

    /**
     * 测试按目录 ID 查询设备
     */
    @Test
    public void testGetDeviceListByCatalogueId() {
        // 先获取一个目录
        List<DeviceCatalogue> catalogues = catalogueService.list();
        if (!catalogues.isEmpty()) {
            String catalogueId = catalogues.get(0).getId();
            
            Page<Device> page = new Page<>(1, 10);
            Page<Device> result = deviceService.getDeviceList(page, null, catalogueId);
            
            assertNotNull(result, "查询结果为空");
            System.out.println("目录 ID " + catalogueId + " 下有 " + result.getTotal() + " 个设备");
            
            for (Device device : result.getRecords()) {
                System.out.println("设备名称：" + device.getDeviceName() + 
                                 ", 目录 ID: " + device.getCatalogueId());
            }
        }
    }

    /**
     * 测试根据目录 ID 查询设备列表（不分页）
     */
    @Test
    public void testGetDevicesByCatalogueId() {
        // 先获取一个目录
        List<DeviceCatalogue> catalogues = catalogueService.list();
        if (!catalogues.isEmpty()) {
            String catalogueId = catalogues.get(0).getId();
            
            List<Device> devices = deviceService.getDevicesByCatalogueId(catalogueId);
            
            assertNotNull(devices, "查询结果为空");
            System.out.println("目录 ID " + catalogueId + " 下有 " + devices.size() + " 个设备");
            
            for (Device device : devices) {
                System.out.println("设备名称：" + device.getDeviceName());
            }
        }
    }

    /**
     * 测试编辑设备
     */
    @Test
    public void testEditDevice() {
        // 先添加一个设备
        Device device = new Device();
        device.setDeviceName("修改前的名称");
        device.setDeviceType("原始类型");
        deviceService.addDevice(device);
        
        // 修改设备信息
        device.setDeviceName("修改后的名称");
        device.setDeviceType("新类型");
        device.setStatus("在线");
        
        boolean result = deviceService.editDevice(device);
        assertTrue(result, "修改设备失败");
        System.out.println("设备修改成功");
        
        // 验证修改
        Device updated = deviceService.getById(device.getId());
        assertEquals("修改后的名称", updated.getDeviceName(), "修改后的名称不匹配");
        assertEquals("新类型", updated.getDeviceType(), "修改后的类型不匹配");
    }

    /**
     * 测试删除设备
     */
    @Test
    public void testDeleteDevice() {
        // 先添加一个设备
        Device device = new Device();
        device.setDeviceName("待删除的设备");
        deviceService.addDevice(device);
        
        // 删除设备
        boolean result = deviceService.deleteDevice(device.getId());
        assertTrue(result, "删除设备失败");
        System.out.println("设备删除成功，ID: " + device.getId());
        
        // 验证删除
        Device deleted = deviceService.getById(device.getId());
        assertNull(deleted, "设备未被删除");
    }
}
