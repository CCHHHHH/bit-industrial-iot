package com.bit.iot.simulator.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.simulator.model.entity.SimulatorTask;
import com.bit.iot.simulator.model.request.SimulatorTaskRequest;
import com.bit.iot.simulator.model.vo.SimulatorTaskVO;
import com.bit.iot.simulator.service.ISimulatorTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/simulator/task")
@Tag(name = "设备模拟-任务管理")
/**
 * 模拟任务管理控制器。
 */
public class SimulatorTaskController extends BaseController {

    /**
     * 模拟任务服务。
     */
    private final ISimulatorTaskService taskService;

    /**
     * 构造模拟任务管理控制器。
     *
     * @param taskService 模拟任务服务
     */
    public SimulatorTaskController(ISimulatorTaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 分页查询模拟任务。
     *
     * @param current 当前页
     * @param size 每页条数
     * @param keyword 关键字
     * @param protocolType 协议类型
     * @param taskStatus 任务状态
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询模拟任务")
    public Result<List<SimulatorTaskVO>> list(@RequestParam(defaultValue = "1") Integer current,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              String keyword,
                                              String protocolType,
                                              String taskStatus) {
        Page<SimulatorTask> page = taskService.getTaskPage(new Page<>(current, size), keyword, protocolType, taskStatus);
        Page<SimulatorTaskVO> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    /**
     * 新增模拟任务。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    @PostMapping
    @Operation(summary = "新增模拟任务")
    public Result<Void> add(@Valid @RequestBody SimulatorTaskRequest request) {
        SimulatorTask task = new SimulatorTask();
        BeanUtils.copyProperties(request, task);
        return taskService.addTask(task) ? success("新增成功") : error("新增失败");
    }

    /**
     * 修改模拟任务。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    @PutMapping
    @Operation(summary = "修改模拟任务")
    public Result<Void> edit(@Valid @RequestBody SimulatorTaskRequest request) {
        SimulatorTask task = new SimulatorTask();
        BeanUtils.copyProperties(request, task);
        return taskService.editTask(task) ? success("修改成功") : error("修改失败");
    }

    /**
     * 删除模拟任务。
     *
     * @param id 任务主键
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除模拟任务")
    public Result<Void> delete(@PathVariable String id) {
        return taskService.deleteTask(id) ? success("删除成功") : error("删除失败");
    }

    /**
     * 启动模拟任务。
     *
     * @param id 任务主键
     * @return 处理结果
     */
    @PutMapping("/{id}/start")
    @Operation(summary = "启动模拟任务")
    public Result<Void> start(@PathVariable String id) {
        return taskService.startTask(id) ? success("启动成功") : error("启动失败");
    }

    /**
     * 停止模拟任务。
     *
     * @param id 任务主键
     * @return 处理结果
     */
    @PutMapping("/{id}/stop")
    @Operation(summary = "停止模拟任务")
    public Result<Void> stop(@PathVariable String id) {
        return taskService.stopTask(id) ? success("停止成功") : error("停止失败");
    }

    /**
     * 手动触发一次模拟任务。
     *
     * @param id 任务主键
     * @return 处理结果
     */
    @PutMapping("/{id}/trigger")
    @Operation(summary = "手动触发一次模拟任务")
    public Result<Void> trigger(@PathVariable String id) {
        return taskService.triggerOnce(id) ? success("触发成功") : error("触发失败");
    }

    /**
     * 将实体转换为视图对象。
     *
     * @param task 任务实体
     * @return 任务视图对象
     */
    private SimulatorTaskVO toVO(SimulatorTask task) {
        SimulatorTaskVO vo = new SimulatorTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
