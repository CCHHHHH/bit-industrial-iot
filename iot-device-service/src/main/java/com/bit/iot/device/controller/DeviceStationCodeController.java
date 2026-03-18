package com.bit.iot.device.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.DeviceStationCode;
import com.bit.iot.device.service.IDeviceStationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 设备测点表 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@RestController
@RequestMapping("/device-station-code")
@Tag(name = "设备测点管理接口", description = "设备测点相关操作接口")
public class DeviceStationCodeController extends BaseController {

    @Autowired
    private IDeviceStationCodeService stationCodeService;

    @GetMapping("/list")
    @Operation(summary = "分页查询设备测点列表")
    public Result<List<DeviceStationCode>> getStationCodeList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String deviceId) {
        Page<DeviceStationCode> page = new Page<>(current, size);
        Page<DeviceStationCode> result = stationCodeService.getStationCodeList(page, deviceId);
        return success(result);
    }

    @PostMapping
    @Operation(summary = "新增设备测点")
    public Result<Void> addStationCode(@RequestBody DeviceStationCode stationCode) {
        boolean success = stationCodeService.addStationCode(stationCode);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑设备测点")
    public Result<Void> editStationCode(@RequestBody DeviceStationCode stationCode) {
        boolean success = stationCodeService.editStationCode(stationCode);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除设备测点")
    public Result<Void> deleteStationCode(@PathVariable String id) {
        boolean success = stationCodeService.deleteStationCode(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @GetMapping("/by-device/{deviceId}")
    @Operation(summary = "根据设备 ID 查询测点列表")
    public Result<List<DeviceStationCode>> getStationCodesByDeviceId(@PathVariable String deviceId) {
        List<DeviceStationCode> stationCodes = stationCodeService.getStationCodesByDeviceId(deviceId);
        return success(stationCodes);
    }
}
