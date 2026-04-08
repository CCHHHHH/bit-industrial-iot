package com.bit.iot.simulator.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.simulator.model.entity.SimulatorDevice;
import com.bit.iot.simulator.model.request.SimulatorDeviceRequest;
import com.bit.iot.simulator.model.vo.SimulatorDeviceVO;
import com.bit.iot.simulator.service.ISimulatorDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/simulator/device")
@Tag(name = "设备模拟-设备管理")
/**
 * 模拟设备管理控制器。
 */
public class SimulatorDeviceController extends BaseController {

    /**
     * 模拟设备服务。
     */
    private final ISimulatorDeviceService deviceService;

    /**
     * 构造模拟设备管理控制器。
     *
     * @param deviceService 模拟设备服务
     */
    public SimulatorDeviceController(ISimulatorDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 分页查询模拟设备。
     *
     * @param current 当前页
     * @param size 每页条数
     * @param keyword 关键字
     * @param deviceType 设备类型
     * @param deviceStatus 设备状态
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询模拟设备")
    public Result<List<SimulatorDeviceVO>> list(@RequestParam(defaultValue = "1") Integer current,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                String keyword,
                                                String deviceType,
                                                String deviceStatus) {
        Page<SimulatorDevice> page = deviceService.getDevicePage(new Page<>(current, size), keyword, deviceType, deviceStatus);
        Page<SimulatorDeviceVO> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    /**
     * 新增模拟设备。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    @PostMapping
    @Operation(summary = "新增模拟设备")
    public Result<Void> add(@Valid @RequestBody SimulatorDeviceRequest request) {
        SimulatorDevice device = new SimulatorDevice();
        BeanUtils.copyProperties(request, device);
        return deviceService.addDevice(device) ? success("新增成功") : error("新增失败");
    }

    /**
     * 修改模拟设备。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    @PutMapping
    @Operation(summary = "修改模拟设备")
    public Result<Void> edit(@Valid @RequestBody SimulatorDeviceRequest request) {
        SimulatorDevice device = new SimulatorDevice();
        BeanUtils.copyProperties(request, device);
        return deviceService.editDevice(device) ? success("修改成功") : error("修改失败");
    }

    /**
     * 删除模拟设备。
     *
     * @param id 设备主键
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除模拟设备")
    public Result<Void> delete(@PathVariable String id) {
        return deviceService.deleteDevice(id) ? success("删除成功") : error("删除失败");
    }

    /**
     * 将实体转换为视图对象。
     *
     * @param device 设备实体
     * @return 设备视图对象
     */
    private SimulatorDeviceVO toVO(SimulatorDevice device) {
        SimulatorDeviceVO vo = new SimulatorDeviceVO();
        BeanUtils.copyProperties(device, vo);
        return vo;
    }
}
