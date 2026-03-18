package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.Device;
import com.bit.iot.device.model.entity.DeviceEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备事件服务测试类
 */
@SpringBootTest
public class DeviceEventServiceTest {

    @Autowired
    private IDeviceEventService eventService;

    @Autowired
    private IDeviceService deviceService;

    /**
     * 测试新增设备事件
     */
    @Test
    public void testAddEvent() {
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
        
        // 创建事件 1
        DeviceEvent event1 = new DeviceEvent();
        event1.setDeviceId(deviceId);
        event1.setEventMessage("设备启动成功");
        
        boolean result = eventService.addEvent(event1);
        assertTrue(result, "添加事件失败");
        System.out.println("事件 1 添加成功，ID: " + event1.getId());

        // 创建事件 2
        DeviceEvent event2 = new DeviceEvent();
        event2.setDeviceId(deviceId);
        event2.setEventMessage("设备运行正常");
        
        result = eventService.addEvent(event2);
        assertTrue(result, "添加事件失败");
        System.out.println("事件 2 添加成功，ID: " + event2.getId());

        // 创建事件 3
        DeviceEvent event3 = new DeviceEvent();
        event3.setDeviceId(deviceId);
        event3.setEventMessage("设备温度过高告警");
        
        result = eventService.addEvent(event3);
        assertTrue(result, "添加事件失败");
        System.out.println("事件 3 添加成功，ID: " + event3.getId());
    }

    /**
     * 测试分页查询设备事件列表
     */
    @Test
    public void testGetEventList() {
        Page<DeviceEvent> page = new Page<>(1, 10);
        Page<DeviceEvent> result = eventService.getEventList(page, null);
        
        assertNotNull(result, "查询结果为空");
        System.out.println("总记录数：" + result.getTotal());
        System.out.println("当前页码：" + result.getCurrent());
        System.out.println("每页大小：" + result.getSize());
        
        for (DeviceEvent event : result.getRecords()) {
            System.out.println("事件 ID: " + event.getId() + 
                             ", 设备 ID: " + event.getDeviceId() + 
                             ", 消息：" + event.getEventMessage() +
                             ", 时间：" + event.getEventTime());
        }
    }

    /**
     * 测试按设备 ID 查询事件
     */
    @Test
    public void testGetEventListByDeviceId() {
        // 先获取一个设备 ID
        List<Device> devices = deviceService.list();
        if (!devices.isEmpty()) {
            String deviceId = devices.get(0).getId();
            
            Page<DeviceEvent> page = new Page<>(1, 10);
            Page<DeviceEvent> result = eventService.getEventList(page, deviceId);
            
            assertNotNull(result, "查询结果为空");
            System.out.println("设备 ID " + deviceId + " 有 " + result.getTotal() + " 个事件");
            
            for (DeviceEvent event : result.getRecords()) {
                System.out.println("事件消息：" + event.getEventMessage() + 
                                 ", 时间：" + event.getEventTime());
            }
        }
    }

    /**
     * 测试根据设备 ID 查询事件列表（不分页）
     */
    @Test
    public void testGetEventsByDeviceId() {
        // 先获取一个设备 ID
        List<Device> devices = deviceService.list();
        if (!devices.isEmpty()) {
            String deviceId = devices.get(0).getId();
            
            List<DeviceEvent> events = eventService.getEventsByDeviceId(deviceId);
            
            assertNotNull(events, "查询结果为空");
            System.out.println("设备 ID " + deviceId + " 有 " + events.size() + " 个事件");
            
            for (DeviceEvent event : events) {
                System.out.println("事件消息：" + event.getEventMessage());
            }
        }
    }

    /**
     * 测试编辑设备事件
     */
    @Test
    public void testEditEvent() {
        // 先获取一个事件
        List<DeviceEvent> events = eventService.list();
        if (!events.isEmpty()) {
            DeviceEvent event = events.get(0);
            
            // 修改事件内容
            event.setEventMessage("修改后的事件消息");
            
            boolean result = eventService.editEvent(event);
            assertTrue(result, "修改事件失败");
            System.out.println("事件修改成功");
            
            // 验证修改
            DeviceEvent updated = eventService.getById(event.getId());
            assertEquals("修改后的事件消息", updated.getEventMessage(), "修改后的消息不匹配");
        } else {
            System.out.println("没有可修改的事件");
        }
    }

    /**
     * 测试删除设备事件
     */
    @Test
    public void testDeleteEvent() {
        // 先添加一个事件
        List<Device> devices = deviceService.list();
        if (!devices.isEmpty()) {
            DeviceEvent event = new DeviceEvent();
            event.setDeviceId(devices.get(0).getId());
            event.setEventMessage("待删除的事件");
            eventService.addEvent(event);
            
            // 删除事件
            boolean result = eventService.deleteEvent(event.getId());
            assertTrue(result, "删除事件失败");
            System.out.println("事件删除成功，ID: " + event.getId());
            
            // 验证删除
            DeviceEvent deleted = eventService.getById(event.getId());
            assertNull(deleted, "事件未被删除");
        }
    }
}
