package com.bit.iot.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.integration.model.entity.IntegrationDataMapping;
import com.bit.iot.integration.dao.IntegrationDataMappingMapper;
import com.bit.iot.integration.model.enums.SchedulerUnitEnum;
import com.bit.iot.integration.service.IIntegrationDataMappingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        return this.save(dataMapping);
    }
    
    @Override
    public boolean editDataMapping(IntegrationDataMapping dataMapping) {
        return this.updateById(dataMapping);
    }
    
    @Override
    public boolean deleteDataMapping(String id) {
        return this.removeById(id);
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
