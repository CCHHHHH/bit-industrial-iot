package com.bit.iot.simulator.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.bit.iot.simulator.model.entity.SimulatorPoint;
import com.bit.iot.simulator.model.request.SimulatorPointRequest;
import com.bit.iot.simulator.model.vo.SimulatorPointVO;
import com.bit.iot.simulator.service.ISimulatorPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/simulator/point")
@Tag(name = "设备模拟-测点管理")
/**
 * 模拟测点管理控制器。
 */
public class SimulatorPointController extends BaseController {

    /**
     * 模拟测点服务。
     */
    private final ISimulatorPointService pointService;

    /**
     * 构造模拟测点管理控制器。
     *
     * @param pointService 模拟测点服务
     */
    public SimulatorPointController(ISimulatorPointService pointService) {
        this.pointService = pointService;
    }

    /**
     * 查询设备测点列表。
     *
     * @param deviceId 设备主键
     * @return 测点列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询设备测点列表")
    public Result<List<SimulatorPointVO>> list(@RequestParam String deviceId) {
        return success(pointService.getPointsByDeviceId(deviceId).stream().map(this::toVO).toList());
    }

    /**
     * 新增设备测点。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    @PostMapping
    @Operation(summary = "新增设备测点")
    public Result<Void> add(@Valid @RequestBody SimulatorPointRequest request) {
        SimulatorPoint point = new SimulatorPoint();
        BeanUtils.copyProperties(request, point);
        return pointService.addPoint(point) ? success("新增成功") : error("新增失败");
    }

    /**
     * 修改设备测点。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    @PutMapping
    @Operation(summary = "修改设备测点")
    public Result<Void> edit(@Valid @RequestBody SimulatorPointRequest request) {
        SimulatorPoint point = new SimulatorPoint();
        BeanUtils.copyProperties(request, point);
        return pointService.editPoint(point) ? success("修改成功") : error("修改失败");
    }

    /**
     * 删除设备测点。
     *
     * @param id 测点主键
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除设备测点")
    public Result<Void> delete(@PathVariable String id) {
        return pointService.deletePoint(id) ? success("删除成功") : error("删除失败");
    }

    /**
     * 将实体转换为视图对象。
     *
     * @param point 测点实体
     * @return 测点视图对象
     */
    private SimulatorPointVO toVO(SimulatorPoint point) {
        SimulatorPointVO vo = new SimulatorPointVO();
        BeanUtils.copyProperties(point, vo);
        return vo;
    }
}
