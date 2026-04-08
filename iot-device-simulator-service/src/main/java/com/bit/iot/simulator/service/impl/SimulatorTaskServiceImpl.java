package com.bit.iot.simulator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.simulator.dao.SimulatorTaskMapper;
import com.bit.iot.simulator.model.entity.SimulatorTask;
import com.bit.iot.simulator.model.enums.TaskStatusEnum;
import com.bit.iot.simulator.scheduler.SimulatorTaskRuntimeManager;
import com.bit.iot.simulator.service.ISimulatorTaskService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
/**
 * 模拟任务服务实现。
 */
public class SimulatorTaskServiceImpl extends ServiceImpl<SimulatorTaskMapper, SimulatorTask>
        implements ISimulatorTaskService {

    /**
     * 模拟任务运行时管理器。
     */
    private final SimulatorTaskRuntimeManager runtimeManager;

    /**
     * 构造模拟任务服务实现。
     *
     * @param runtimeManager 模拟任务运行时管理器
     */
    public SimulatorTaskServiceImpl(SimulatorTaskRuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

    /**
     * 分页查询模拟任务。
     *
     * @param page 分页参数
     * @param keyword 关键字
     * @param protocolType 协议类型
     * @param taskStatus 任务状态
     * @return 分页结果
     */
    @Override
    public Page<SimulatorTask> getTaskPage(Page<SimulatorTask> page, String keyword, String protocolType, String taskStatus) {
        QueryWrapper<SimulatorTask> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like("task_name", keyword).or().like("device_id", keyword));
        }
        if (protocolType != null && !protocolType.isBlank()) {
            queryWrapper.eq("protocol_type", protocolType);
        }
        if (taskStatus != null && !taskStatus.isBlank()) {
            queryWrapper.eq("task_status", taskStatus);
        }
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }

    /**
     * 新增模拟任务。
     *
     * @param task 任务实体
     * @return 是否成功
     */
    @Override
    public boolean addTask(SimulatorTask task) {
        Date now = new Date();
        task.setTaskStatus(TaskStatusEnum.STOPPED.name());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return this.save(task);
    }

    /**
     * 修改模拟任务并在任务运行时重新加载调度。
     *
     * @param task 任务实体
     * @return 是否成功
     */
    @Override
    public boolean editTask(SimulatorTask task) {
        task.setUpdateTime(new Date());
        boolean updated = this.updateById(task);
        // 重新查询最新任务配置，避免使用部分更新对象导致调度参数不完整。
        SimulatorTask latest = this.getById(task.getId());
        if (updated && latest != null && TaskStatusEnum.RUNNING.name().equals(latest.getTaskStatus())) {
            // 运行中的任务修改后立即重建调度，使新配置立刻生效。
            runtimeManager.scheduleTask(latest);
        }
        return updated;
    }

    /**
     * 删除模拟任务并取消已有调度。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    @Override
    public boolean deleteTask(String id) {
        runtimeManager.cancelTask(id);
        return this.removeById(id);
    }

    /**
     * 启动模拟任务。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    @Override
    public boolean startTask(String id) {
        SimulatorTask task = this.getById(id);
        if (task == null) {
            throw new RuntimeException("模拟任务不存在");
        }
        task.setTaskStatus(TaskStatusEnum.RUNNING.name());
        task.setUpdateTime(new Date());
        boolean updated = this.updateById(task);
        if (updated) {
            // 仅在数据库状态更新成功后启动调度，保证状态与运行时一致。
            runtimeManager.scheduleTask(task);
        }
        return updated;
    }

    /**
     * 停止模拟任务。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    @Override
    public boolean stopTask(String id) {
        SimulatorTask task = this.getById(id);
        if (task == null) {
            throw new RuntimeException("模拟任务不存在");
        }
        task.setTaskStatus(TaskStatusEnum.STOPPED.name());
        task.setUpdateTime(new Date());
        // 先取消调度，避免停止过程中再次触发执行。
        runtimeManager.cancelTask(id);
        return this.updateById(task);
    }

    /**
     * 手动触发一次模拟任务。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    @Override
    public boolean triggerOnce(String id) {
        SimulatorTask task = this.getById(id);
        if (task == null) {
            throw new RuntimeException("模拟任务不存在");
        }
        runtimeManager.triggerOnce(id);
        return true;
    }
}
