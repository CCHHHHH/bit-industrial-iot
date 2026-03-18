package com.bit.iot.device.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.DeviceEvent;
import com.bit.iot.device.service.IDeviceEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 设备事件表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@RestController
@RequestMapping("/device-event")
@Tag(name = "设备事件管理接口", description = "设备事件相关操作接口")
public class DeviceEventController extends BaseController {

    @Autowired
    private IDeviceEventService eventService;

    @GetMapping("/list")
    @Operation(summary = "分页查询设备事件列表")
    public Result<List<DeviceEvent>> getEventList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String deviceId) {
        Page<DeviceEvent> page = new Page<>(current, size);
        Page<DeviceEvent> result = eventService.getEventList(page, deviceId);
        return success(result);
    }

    @PostMapping
    @Operation(summary = "新增设备事件")
    public Result<Void> addEvent(@RequestBody DeviceEvent event) {
        boolean success = eventService.addEvent(event);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑设备事件")
    public Result<Void> editEvent(@RequestBody DeviceEvent event) {
        boolean success = eventService.editEvent(event);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除设备事件")
    public Result<Void> deleteEvent(@PathVariable String id) {
        boolean success = eventService.deleteEvent(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @GetMapping("/by-device/{deviceId}")
    @Operation(summary = "根据设备 ID 查询事件列表")
    public Result<List<DeviceEvent>> getEventsByDeviceId(@PathVariable String deviceId) {
        List<DeviceEvent> events = eventService.getEventsByDeviceId(deviceId);
        return success(events);
    }
}
