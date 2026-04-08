package com.bit.iot.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.integration.model.entity.IntegrationConfigParam;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 集成实例配置参数表 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-20 11:32:38
 */
public interface IIntegrationConfigParamService extends IService<IntegrationConfigParam> {
    
    /**
     * 根据集成配置 ID 查询参数列表
     * @param integrationId 集成配置 ID
     * @return 参数列表
     */
    List<IntegrationConfigParam> getConfigParamsByIntegrationId(String integrationId);

    /**
     * 新增配置参数
     * @param configParam 参数信息
     * @return 是否成功
     */
    boolean addConfigParam(IntegrationConfigParam configParam);

    /**
     * 编辑配置参数
     * @param configParam 参数信息
     * @return 是否成功
     */
    boolean editConfigParam(IntegrationConfigParam configParam);

    /**
     * 删除配置参数
     * @param id 参数 ID
     * @return 是否成功
     */
    boolean deleteConfigParam(String id);

    /**
     * 批量保存集成实例配置参数（先删后存）
     * @param integrationId 集成实例 ID
     * @param paramList     参数列表
     * @return 是否成功
     */
    boolean saveConfigParams(String integrationId, List<IntegrationConfigParam> paramList);

    /**
     * 以 key/value 形式读取实例参数
     * @param integrationId 集成实例 ID
     * @return 参数 map
     */
    Map<String, String> getConfigParamMap(String integrationId);

    /**
     * 以 key/value 形式覆盖保存实例参数
     * @param integrationId 集成实例 ID
     * @param configMap 参数 map
     * @return 是否成功
     */
    boolean saveConfigParamMap(String integrationId, Map<String, String> configMap);

    /**
     * 覆盖保存带描述的实例参数
     * @param integrationId 集成实例 ID
     * @param paramList 参数列表
     * @return 是否成功
     */
    boolean saveConfigParamItems(String integrationId, List<IntegrationConfigParam> paramList);

}
