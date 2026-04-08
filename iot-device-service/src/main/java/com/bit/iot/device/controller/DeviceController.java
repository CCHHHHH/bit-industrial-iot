package com.bit.iot.device.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.Device;
import com.bit.iot.device.model.request.DeviceRequest;
import com.bit.iot.device.model.vo.DeviceVO;
import com.bit.iot.device.service.IDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 设备表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@RestController
@RequestMapping("/device")
@Tag(name = "设备管理接口", description = "设备相关操作接口")
public class DeviceController extends BaseController {

    @Autowired
    private IDeviceService deviceService;

    @GetMapping("/list")
    @Operation(summary = "分页查询设备列表")
    public Result<List<DeviceVO>> getDeviceList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String deviceName,
            String catalogueId) {
        Page<Device> page = new Page<>(current, size);
        Page<Device> result = deviceService.getDeviceList(page, deviceName, catalogueId);
        Page<DeviceVO> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    @PostMapping
    @Operation(summary = "新增设备")
    public Result<Void> addDevice(@RequestBody DeviceRequest device) {
        Device entity = new Device();
        BeanUtils.copyProperties(device, entity);
        boolean success = deviceService.addDevice(entity);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑设备")
    public Result<Void> editDevice(@RequestBody DeviceRequest device) {
        Device entity = new Device();
        BeanUtils.copyProperties(device, entity);
        boolean success = deviceService.editDevice(entity);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除设备")
    public Result<Void> deleteDevice(@PathVariable String id) {
        boolean success = deviceService.deleteDevice(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @GetMapping("/by-catalogue/{catalogueId}")
    @Operation(summary = "根据设备目录 ID 查询设备列表")
    public Result<List<DeviceVO>> getDevicesByCatalogueId(@PathVariable String catalogueId) {
        List<Device> devices = deviceService.getDevicesByCatalogueId(catalogueId);
        return success(devices.stream().map(this::toVO).toList());
    }

    private DeviceVO toVO(Device device) {
        DeviceVO vo = new DeviceVO();
        BeanUtils.copyProperties(device, vo);
        return vo;
    }
}
