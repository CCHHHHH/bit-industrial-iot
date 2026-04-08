package com.bit.iot.simulator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.simulator.model.entity.SimulatorTask;

/**
 * 模拟任务服务接口。
 */
public interface ISimulatorTaskService extends IService<SimulatorTask> {
    /**
     * 分页查询模拟任务。
     *
     * @param page 分页参数
     * @param keyword 关键字
     * @param protocolType 协议类型
     * @param taskStatus 任务状态
     * @return 分页结果
     */
    Page<SimulatorTask> getTaskPage(Page<SimulatorTask> page, String keyword, String protocolType, String taskStatus);

    /**
     * 新增模拟任务。
     *
     * @param task 任务实体
     * @return 是否成功
     */
    boolean addTask(SimulatorTask task);

    /**
     * 修改模拟任务。
     *
     * @param task 任务实体
     * @return 是否成功
     */
    boolean editTask(SimulatorTask task);

    /**
     * 删除模拟任务。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    boolean deleteTask(String id);

    /**
     * 启动模拟任务。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    boolean startTask(String id);

    /**
     * 停止模拟任务。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    boolean stopTask(String id);

    /**
     * 立即触发一次模拟任务。
     *
     * @param id 任务主键
     * @return 是否成功
     */
    boolean triggerOnce(String id);
}
