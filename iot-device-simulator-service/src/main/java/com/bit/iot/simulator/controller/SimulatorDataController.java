package com.bit.iot.simulator.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.simulator.model.entity.SimulatorSendLog;
import com.bit.iot.simulator.model.entity.SimulatorTimeseriesData;
import com.bit.iot.simulator.model.request.SimulatorDataHistoryRequest;
import com.bit.iot.simulator.model.vo.SimulatorSendLogVO;
import com.bit.iot.simulator.model.vo.SimulatorTimeseriesDataVO;
import com.bit.iot.simulator.service.ISimulatorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/simulator/data")
@Tag(name = "设备模拟-数据查询")
/**
 * 模拟数据查询控制器。
 */
public class SimulatorDataController extends BaseController {

    /**
     * 模拟数据查询服务。
     */
    private final ISimulatorDataService dataService;

    /**
     * 构造模拟数据查询控制器。
     *
     * @param dataService 模拟数据查询服务
     */
    public SimulatorDataController(ISimulatorDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * 查询设备最新测点值。
     *
     * @param deviceId 设备主键
     * @return 最新测点值列表
     */
    @GetMapping("/latest")
    @Operation(summary = "查询设备最新测点值")
    public Result<List<SimulatorTimeseriesDataVO>> latest(@RequestParam String deviceId) {
        return success(dataService.getLatestByDeviceId(deviceId).stream().map(this::toVO).toList());
    }

    /**
     * 分页查询模拟时序历史。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/history")
    @Operation(summary = "分页查询模拟时序历史")
    public Result<List<SimulatorTimeseriesDataVO>> history(@RequestBody SimulatorDataHistoryRequest request) {
        Page<SimulatorTimeseriesData> page = dataService.getHistory(
                new Page<>(request.getCurrent(), request.getSize()),
                request.getDeviceId(),
                request.getPointCode(),
                request.getProtocolType());
        Page<SimulatorTimeseriesDataVO> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    /**
     * 分页查询发送日志。
     *
     * @param current 当前页
     * @param size 每页条数
     * @param taskId 任务主键
     * @param protocolType 协议类型
     * @param sendStatus 发送状态
     * @return 分页结果
     */
    @GetMapping("/send-log")
    @Operation(summary = "分页查询发送日志")
    public Result<List<SimulatorSendLogVO>> sendLog(@RequestParam(defaultValue = "1") Integer current,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    String taskId,
                                                    String protocolType,
                                                    String sendStatus) {
        Page<SimulatorSendLog> page = dataService.getSendLogPage(new Page<>(current, size), taskId, protocolType, sendStatus);
        Page<SimulatorSendLogVO> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(page.getRecords().stream().map(this::toSendLogVO).toList());
        return success(responsePage);
    }

    /**
     * 将实体转换为时序数据视图对象。
     *
     * @param data 时序数据实体
     * @return 时序数据视图对象
     */
    private SimulatorTimeseriesDataVO toVO(SimulatorTimeseriesData data) {
        SimulatorTimeseriesDataVO vo = new SimulatorTimeseriesDataVO();
        BeanUtils.copyProperties(data, vo);
        return vo;
    }

    /**
     * 将实体转换为发送日志视图对象。
     *
     * @param log 发送日志实体
     * @return 发送日志视图对象
     */
    private SimulatorSendLogVO toSendLogVO(SimulatorSendLog log) {
        SimulatorSendLogVO vo = new SimulatorSendLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
