package com.bit.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.Device;
import com.bit.iot.device.model.entity.DeviceCatalogue;
import com.bit.iot.device.dao.DeviceCatalogueMapper;
import com.bit.iot.device.service.IDeviceCatalogueService;
import com.bit.iot.device.service.IDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 设备目录 服务实现类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Service
public class DeviceCatalogueServiceImpl extends ServiceImpl<DeviceCatalogueMapper, DeviceCatalogue> implements IDeviceCatalogueService {

    @Autowired
    private IDeviceService deviceService;

    @Override
    public Page<DeviceCatalogue> getCatalogueList(Page<DeviceCatalogue> page, String catalogueName) {
        QueryWrapper<DeviceCatalogue> queryWrapper = new QueryWrapper<>();
        if (catalogueName != null && !catalogueName.isEmpty()) {
            queryWrapper.like("catalogue_name", catalogueName);
        }
        queryWrapper.orderByAsc("parent_id", "create_time");
        return this.page(page, queryWrapper);
    }
    
    @Override
    public boolean addCatalogue(DeviceCatalogue catalogue) {
        Date now = new Date();
        catalogue.setCreateTime(now);
        catalogue.setUpdateTime(now);
        return this.save(catalogue);
    }
    
    @Override
    public boolean editCatalogue(DeviceCatalogue catalogue) {
        Date now = new Date();
        catalogue.setUpdateTime(now);
        return this.updateById(catalogue);
    }
    
    @Override
    public boolean deleteCatalogue(String id) {
        return this.removeById(id);
    }
    
    @Override
    public List<DeviceCatalogue> getTreeCatalogues() {
        // 查询所有目录
        List<DeviceCatalogue> allCatalogues = this.list();
        
        // 找到根节点（parent_id 为 null 或空）
        List<DeviceCatalogue> rootNodes = allCatalogues.stream()
                .filter(c -> c.getParentId() == null || c.getParentId().isEmpty())
                .collect(Collectors.toList());
        
        // 递归构建树形结构并统计设备数量
        buildTree(rootNodes, allCatalogues);
        
        return rootNodes;
    }
    
    /**
     * 递归构建树形结构并统计设备数量
     */
    private void buildTree(List<DeviceCatalogue> parentNodes, List<DeviceCatalogue> allNodes) {
        for (DeviceCatalogue parentNode : parentNodes) {
            // 1. 统计当前目录直接挂载的设备数量
            QueryWrapper<Device> deviceQueryWrapper = new QueryWrapper<>();
            deviceQueryWrapper.eq("catalogue_id", parentNode.getId());
            Long deviceCount = deviceService.count(deviceQueryWrapper);
            parentNode.setDeviceCount(deviceCount);
            
            // 2. 找到当前节点的所有子节点
            List<DeviceCatalogue> children = allNodes.stream()
                    .filter(node -> parentNode.getId().equals(node.getParentId()))
                    .collect(Collectors.toList());
            
            if (!children.isEmpty()) {
                // 将子节点设置到父节点的 children 属性中
                parentNode.setChildren(children);
                // 递归处理子节点
                buildTree(children, allNodes);
            }
        }
    }

}
