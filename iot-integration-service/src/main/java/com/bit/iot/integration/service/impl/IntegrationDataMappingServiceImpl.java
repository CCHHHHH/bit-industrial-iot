package com.bit.iot.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.bit.iot.integration.dao.IntegrationDataMappingMapper;
import com.bit.iot.integration.model.enums.SchedulerUnitEnum;
import com.bit.iot.integration.scheduler.IntegrationCollectScheduler;
import com.bit.iot.integration.service.IIntegrationDataMappingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 集成实例数据映射表 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-18 04:52:03
 */
@Service
public class IntegrationDataMappingServiceImpl extends ServiceImpl<IntegrationDataMappingMapper, IntegrationDataMapping> implements IIntegrationDataMappingService {

    private final IntegrationCollectScheduler collectScheduler;

    public IntegrationDataMappingServiceImpl(@Lazy IntegrationCollectScheduler collectScheduler) {
        this.collectScheduler = collectScheduler;
    }

    @Override
    public Page<IntegrationDataMapping> getDataMappingList(Page<IntegrationDataMapping> page, String integrationId) {
        QueryWrapper<IntegrationDataMapping> queryWrapper = new QueryWrapper<>();
        if (integrationId != null && !integrationId.isEmpty()) {
            queryWrapper.eq("integration_id", integrationId);
        }
        return this.page(page, queryWrapper);
    }
    
    @Override
    public boolean addDataMapping(IntegrationDataMapping dataMapping) {
        boolean saved = this.save(dataMapping);
        if (saved) {
            collectScheduler.restartIfRunning(dataMapping.getIntegrationId());
        }
        return saved;
    }
    
    @Override
    public boolean editDataMapping(IntegrationDataMapping dataMapping) {
        IntegrationDataMapping oldMapping = this.getById(dataMapping.getId());
        boolean updated = this.updateById(dataMapping);
        if (updated) {
            String integrationId = dataMapping.getIntegrationId() != null
                    ? dataMapping.getIntegrationId()
                    : oldMapping == null ? null : oldMapping.getIntegrationId();
            if (oldMapping != null && oldMapping.getIntegrationId() != null
                    && !oldMapping.getIntegrationId().equals(integrationId)) {
                collectScheduler.restartIfRunning(oldMapping.getIntegrationId());
            }
            collectScheduler.restartIfRunning(integrationId);
        }
        return updated;
    }
    
    @Override
    public boolean deleteDataMapping(String id) {
        IntegrationDataMapping oldMapping = this.getById(id);
        boolean removed = this.removeById(id);
        if (removed && oldMapping != null) {
            collectScheduler.restartIfRunning(oldMapping.getIntegrationId());
        }
        return removed;
    }
    
    @Override
    public List<IntegrationDataMapping> getDataMappingsByIntegrationId(String integrationId) {
        QueryWrapper<IntegrationDataMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("integration_id", integrationId);
        return this.list(queryWrapper);
    }

    @Override
    public IntegrationDataMapping getDataMappingByType(String integrationId, String mappingType) {
        QueryWrapper<IntegrationDataMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("integration_id", integrationId)
                    .eq("mapping_type", mappingType);
        return this.getOne(queryWrapper);
    }

    @Override
    public Long calculateSeconds(Long schedulerTime, String schedulerUnit) {
        if (schedulerTime == null || schedulerUnit == null) {
            return null;
        }
        // 使用枚举类进行转换
        return SchedulerUnitEnum.convertToSeconds(schedulerTime, schedulerUnit);
    }

}
