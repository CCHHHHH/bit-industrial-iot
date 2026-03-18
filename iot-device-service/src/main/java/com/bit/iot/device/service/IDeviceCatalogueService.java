package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.device.model.entity.DeviceCatalogue;

import java.util.List;

/**
 * <p>
 * 设备目录 服务类
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
public interface IDeviceCatalogueService extends IService<DeviceCatalogue> {
    
    /**
     * 分页查询设备目录列表
     * @param page 分页信息
     * @param catalogueName 目录名称
     * @return 设备目录列表
     */
    Page<DeviceCatalogue> getCatalogueList(Page<DeviceCatalogue> page, String catalogueName);
    
    /**
     * 新增设备目录
     * @param catalogue 设备目录信息
     * @return 是否成功
     */
    boolean addCatalogue(DeviceCatalogue catalogue);
    
    /**
     * 编辑设备目录
     * @param catalogue 设备目录信息
     * @return 是否成功
     */
    boolean editCatalogue(DeviceCatalogue catalogue);
    
    /**
     * 删除设备目录
     * @param id 目录 ID
     * @return 是否成功
     */
    boolean deleteCatalogue(String id);
    
    /**
     * 查询树形结构的设备目录
     * @return 树形结构的目录列表
     */
    List<DeviceCatalogue> getTreeCatalogues();
    
}
