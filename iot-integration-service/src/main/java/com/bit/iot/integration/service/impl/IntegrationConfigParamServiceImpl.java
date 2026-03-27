package com.bit.iot.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bit.iot.integration.model.entity.IntegrationConfigParam;
import com.bit.iot.integration.dao.IntegrationConfigParamMapper;
import com.bit.iot.integration.service.IIntegrationConfigParamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 集成实例配置参数表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-20 11:32:38
 */
@Service
public class IntegrationConfigParamServiceImpl extends ServiceImpl<IntegrationConfigParamMapper, IntegrationConfigParam> implements IIntegrationConfigParamService {

    @Override
    public List<IntegrationConfigParam> getConfigParamsByIntegrationId(String integrationId) {
        QueryWrapper<IntegrationConfigParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("integration_id", integrationId);
        return this.list(queryWrapper);
    }

    @Override
    public boolean addConfigParam(IntegrationConfigParam configParam) {
        return this.save(configParam);
    }

    @Override
    public boolean editConfigParam(IntegrationConfigParam configParam) {
        return this.updateById(configParam);
    }

    @Override
    public boolean deleteConfigParam(String id) {
        return this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConfigParams(String integrationId, List<IntegrationConfigParam> paramList) {
        // 先删除该集成实例的所有旧参数
        QueryWrapper<IntegrationConfigParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("integration_id", integrationId);
        this.remove(queryWrapper);

        // 批量插入新参数
        if (paramList != null && !paramList.isEmpty()) {
            paramList.forEach(p -> p.setIntegrationId(integrationId));
            return this.saveBatch(paramList);
        }
        return true;
    }

}
