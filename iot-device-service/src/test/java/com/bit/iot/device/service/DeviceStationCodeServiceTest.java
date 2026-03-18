package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.Device;
import com.bit.iot.device.model.entity.DeviceStationCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备测点服务测试类
 */
@SpringBootTest
public class DeviceStationCodeServiceTest {

    @Autowired
    private IDeviceStationCodeService stationCodeService;

    @Autowired
    private IDeviceService deviceService;

    /**
     * 测试新增设备测点
     */
    @Test
    public void testAddStationCode() {
        // 先获取或创建一个设备
        List<Device> devices = deviceService.list();
        String deviceId = "";
        
        if (devices.isEmpty()) {
            // 创建设备
            Device device = new Device();
            device.setDeviceName("测试用设备");
            deviceService.addDevice(device);
            deviceId = device.getId();
        } else {
            deviceId = devices.get(0).getId();
        }
        
        // 创建测点 1
        DeviceStationCode code1 = new DeviceStationCode();
        code1.setDeviceId(deviceId);
        code1.setStationCode("TEMP_001");
        code1.setStationName("温度传感器 1");
        code1.setStandardStationCode("STD_TEMP_001");
        code1.setStationDesc("测量环境温度");
        
        boolean result = stationCodeService.addStationCode(code1);
        assertTrue(result, "添加测点失败");
        System.out.println("测点 1 添加成功，ID: " + code1.getId());

        // 创建测点 2
        DeviceStationCode code2 = new DeviceStationCode();
        code2.setDeviceId(deviceId);
        code2.setStationCode("PRESS_001");
        code2.setStationName("压力传感器 1");
        code2.setStandardStationCode("STD_PRESS_001");
        code2.setStationDesc("测量系统压力");
        
        result = stationCodeService.addStationCode(code2);
        assertTrue(result, "添加测点失败");
        System.out.println("测点 2 添加成功，ID: " + code2.getId());

        // 创建测点 3
        DeviceStationCode code3 = new DeviceStationCode();
        code3.setDeviceId(deviceId);
        code3.setStationCode("FLOW_001");
        code3.setStationName("流量传感器 1");
        code3.setStandardStationCode("STD_FLOW_001");
        code3.setStationDesc("测量液体流量");
        
        result = stationCodeService.addStationCode(code3);
        assertTrue(result, "添加测点失败");
        System.out.println("测点 3 添加成功，ID: " + code3.getId());
    }

    /**
     * 测试分页查询设备测点列表
     */
    @Test
    public void testGetStationCodeList() {
        Page<DeviceStationCode> page = new Page<>(1, 10);
        Page<DeviceStationCode> result = stationCodeService.getStationCodeList(page, null);
        
        assertNotNull(result, "查询结果为空");
        System.out.println("总记录数：" + result.getTotal());
        System.out.println("当前页码：" + result.getCurrent());
        System.out.println("每页大小：" + result.getSize());
        
        for (DeviceStationCode code : result.getRecords()) {
            System.out.println("测点 ID: " + code.getId() + 
                             ", 设备 ID: " + code.getDeviceId() + 
                             ", 编码：" + code.getStationCode() +
                             ", 名称：" + code.getStationName());
        }
    }

    /**
     * 测试按设备 ID 查询测点
     */
    @Test
    public void testGetStationCodeListByDeviceId() {
        // 先获取一个设备 ID
        List<Device> devices = deviceService.list();
        if (!devices.isEmpty()) {
            String deviceId = devices.get(0).getId();
            
            Page<DeviceStationCode> page = new Page<>(1, 10);
            Page<DeviceStationCode> result = stationCodeService.getStationCodeList(page, deviceId);
            
            assertNotNull(result, "查询结果为空");
            System.out.println("设备 ID " + deviceId + " 有 " + result.getTotal() + " 个测点");
            
            for (DeviceStationCode code : result.getRecords()) {
                System.out.println("测点编码：" + code.getStationCode() + 
                                 ", 名称：" + code.getStationName());
            }
        }
    }

    /**
     * 测试根据设备 ID 查询测点列表（不分页）
     */
    @Test
    public void testGetStationCodesByDeviceId() {
        // 先获取一个设备 ID
        List<Device> devices = deviceService.list();
        if (!devices.isEmpty()) {
            String deviceId = devices.get(0).getId();
            
            List<DeviceStationCode> codes = stationCodeService.getStationCodesByDeviceId(deviceId);
            
            assertNotNull(codes, "查询结果为空");
            System.out.println("设备 ID " + deviceId + " 有 " + codes.size() + " 个测点");
            
            for (DeviceStationCode code : codes) {
                System.out.println("测点编码：" + code.getStationCode() + 
                                 ", 名称：" + code.getStationName() +
                                 ", 标准编码：" + code.getStandardStationCode());
            }
        }
    }

    /**
     * 测试编辑设备测点
     */
    @Test
    public void testEditStationCode() {
        // 先获取一个测点
        List<DeviceStationCode> codes = stationCodeService.list();
        if (!codes.isEmpty()) {
            DeviceStationCode code = codes.get(0);
            
            // 修改测点信息
            code.setStationName("修改后的名称");
            code.setStationDesc("修改后的描述");
            
            boolean result = stationCodeService.editStationCode(code);
            assertTrue(result, "修改测点失败");
            System.out.println("测点修改成功");
            
            // 验证修改
            DeviceStationCode updated = stationCodeService.getById(code.getId());
            assertEquals("修改后的名称", updated.getStationName(), "修改后的名称不匹配");
            assertEquals("修改后的描述", updated.getStationDesc(), "修改后的描述不匹配");
        } else {
            System.out.println("没有可修改的测点");
        }
    }

    /**
     * 测试删除设备测点
     */
    @Test
    public void testDeleteStationCode() {
        // 先添加一个测点
        List<Device> devices = deviceService.list();
        if (!devices.isEmpty()) {
            DeviceStationCode code = new DeviceStationCode();
            code.setDeviceId(devices.get(0).getId());
            code.setStationCode("TEST_CODE");
            code.setStationName("待删除的测点");
            stationCodeService.addStationCode(code);
            
            // 删除测点
            boolean result = stationCodeService.deleteStationCode(code.getId());
            assertTrue(result, "删除测点失败");
            System.out.println("测点删除成功，ID: " + code.getId());
            
            // 验证删除
            DeviceStationCode deleted = stationCodeService.getById(code.getId());
            assertNull(deleted, "测点未被删除");
        }
    }

    /**
     * 测试批量添加测点数据
     */
    @Test
    public void testBatchAddStationCodes() {
        // 先获取一个设备 ID
        List<Device> devices = deviceService.list();
        if (!devices.isEmpty()) {
            String deviceId = devices.get(0).getId();
            
            // 批量添加测点
            String[][] testData = {
                {"VOLT_001", "电压传感器 1", "STD_VOLT_001", "测量电压"},
                {"CURR_001", "电流传感器 1", "STD_CURR_001", "测量电流"},
                {"POWR_001", "功率传感器 1", "STD_POWR_001", "测量功率"},
                {"ENER_001", "电能传感器 1", "STD_ENER_001", "测量电能"},
                {"FREQ_001", "频率传感器 1", "STD_FREQ_001", "测量频率"}
            };
            
            int count = 0;
            for (String[] data : testData) {
                DeviceStationCode code = new DeviceStationCode();
                code.setDeviceId(deviceId);
                code.setStationCode(data[0]);
                code.setStationName(data[1]);
                code.setStandardStationCode(data[2]);
                code.setStationDesc(data[3]);
                
                if (stationCodeService.addStationCode(code)) {
                    count++;
                }
            }
            
            System.out.println("成功添加 " + count + " 个测点");
            
            // 验证添加结果
            List<DeviceStationCode> allCodes = stationCodeService.getStationCodesByDeviceId(deviceId);
            System.out.println("设备 ID " + deviceId + " 现在共有 " + allCodes.size() + " 个测点");
        }
    }
}
