package com.bit.iot.integration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;

import java.util.List;

/**
 * <p>
 * 集成实例数据映射表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
public interface IIntegrationDataMappingService extends IService<IntegrationDataMapping> {
    
    /**
     * 分页查询数据映射列表
     * @param page 分页信息
     * @param integrationId 集成配置 ID
     * @return 数据映射列表
     */
    Page<IntegrationDataMapping> getDataMappingList(Page<IntegrationDataMapping> page, String integrationId);
    
    /**
     * 新增数据映射
     * @param dataMapping 数据映射信息
     * @return 是否成功
     */
    boolean addDataMapping(IntegrationDataMapping dataMapping);
    
    /**
     * 编辑数据映射
     * @param dataMapping 数据映射信息
     * @return 是否成功
     */
    boolean editDataMapping(IntegrationDataMapping dataMapping);
    
    /**
     * 删除数据映射
     * @param id 数据映射 ID
     * @return 是否成功
     */
    boolean deleteDataMapping(String id);
    
    /**
     * 根据集成配置 ID 查询数据映射列表
     * @param integrationId 集成配置 ID
     * @return 数据映射列表
     */
    List<IntegrationDataMapping> getDataMappingsByIntegrationId(String integrationId);
    
    /**
     * 根据集成配置 ID 和映射类型查询数据映射
     * @param integrationId 集成配置 ID
     * @param mappingType   映射类型（MappingTypeEnum.code）
     * @return 数据映射，不存在则返回 null
     */
    IntegrationDataMapping getDataMappingByType(String integrationId, String mappingType);

    /**
     * 根据调度时间和单位计算秒数
     * @param schedulerTime 调度时间值
     * @param schedulerUnit 调度单位（d、h、m、s）
     * @return 秒数，如果单位无效返回 null
     */
    Long calculateSeconds(Long schedulerTime, String schedulerUnit);
    
}
