package com.bit.iot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.DeviceCatalogue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备目录服务测试类
 */
@SpringBootTest
public class DeviceCatalogueServiceTest {

    @Autowired
    private IDeviceCatalogueService catalogueService;

    /**
     * 测试新增设备目录
     */
    @Test
    public void testAddCatalogue() {
        // 创建根目录
        DeviceCatalogue root = new DeviceCatalogue();
        root.setParentId("");
        root.setCatalogueName("根目录 - 测试");
        
        boolean result = catalogueService.addCatalogue(root);
        assertTrue(result, "添加根目录失败");
        System.out.println("根目录添加成功，ID: " + root.getId());

        // 创建一级子目录
        DeviceCatalogue child1 = new DeviceCatalogue();
        child1.setParentId(root.getId());
        child1.setCatalogueName("一级子目录 1");
        
        result = catalogueService.addCatalogue(child1);
        assertTrue(result, "添加子目录失败");
        System.out.println("一级子目录 1 添加成功，ID: " + child1.getId());

        // 创建另一个一级子目录
        DeviceCatalogue child2 = new DeviceCatalogue();
        child2.setParentId(root.getId());
        child2.setCatalogueName("一级子目录 2");
        
        result = catalogueService.addCatalogue(child2);
        assertTrue(result, "添加子目录失败");
        System.out.println("一级子目录 2 添加成功，ID: " + child2.getId());

        // 创建二级子目录
        DeviceCatalogue grandChild = new DeviceCatalogue();
        grandChild.setParentId(child1.getId());
        grandChild.setCatalogueName("二级子目录 1-1");
        
        result = catalogueService.addCatalogue(grandChild);
        assertTrue(result, "添加二级子目录失败");
        System.out.println("二级子目录 1-1 添加成功，ID: " + grandChild.getId());
    }

    /**
     * 测试分页查询设备目录
     */
    @Test
    public void testGetCatalogueList() {
        Page<DeviceCatalogue> page = new Page<>(1, 10);
        Page<DeviceCatalogue> result = catalogueService.getCatalogueList(page, null);
        
        assertNotNull(result, "查询结果为空");
        System.out.println("总记录数：" + result.getTotal());
        System.out.println("当前页码：" + result.getCurrent());
        System.out.println("每页大小：" + result.getSize());
        System.out.println("总页数：" + result.getPages());
        
        for (DeviceCatalogue catalogue : result.getRecords()) {
            System.out.println("目录 ID: " + catalogue.getId() + 
                             ", 名称：" + catalogue.getCatalogueName() + 
                             ", 父级 ID: " + catalogue.getParentId());
        }
    }

    /**
     * 测试按名称查询设备目录
     */
    @Test
    public void testGetCatalogueListByName() {
        Page<DeviceCatalogue> page = new Page<>(1, 10);
        Page<DeviceCatalogue> result = catalogueService.getCatalogueList(page, "根目录");
        
        assertNotNull(result, "查询结果为空");
        assertTrue(result.getTotal() > 0, "未找到匹配的目录");
        System.out.println("找到 " + result.getTotal() + " 个包含'根目录'的目录");
        
        for (DeviceCatalogue catalogue : result.getRecords()) {
            System.out.println("目录名称：" + catalogue.getCatalogueName());
        }
    }

    /**
     * 测试树形结构查询
     */
    @Test
    public void testGetTreeCatalogues() {
        List<DeviceCatalogue> treeCatalogues = catalogueService.getTreeCatalogues();
        
        assertNotNull(treeCatalogues, "树形目录为空");
        System.out.println("树形目录根节点数量：" + treeCatalogues.size());
        
        printTree(treeCatalogues, 0);
    }

    /**
     * 递归打印树形结构
     */
    private void printTree(List<DeviceCatalogue> nodes, int level) {
        for (int i = 0; i < nodes.size(); i++) {
            DeviceCatalogue node = nodes.get(i);
            StringBuilder indent = new StringBuilder();
            for (int j = 0; j < level; j++) {
                indent.append("│   ");
            }
            
            // 判断是否是当前层的最后一个节点
            boolean isLast = (i == nodes.size() - 1);
            String prefix = isLast ? "└── " : "├── ";
            
            System.out.println(indent + prefix + node.getCatalogueName() + 
                             " (ID: " + node.getId() + ", ParentID: " + 
                             (node.getParentId() != null ? node.getParentId() : "null") + ")");
            
            // 递归打印子节点
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                printTree(node.getChildren(), level + 1);
            }
        }
    }

    /**
     * 测试编辑设备目录
     */
    @Test
    public void testEditCatalogue() {
        // 先添加一个目录
        DeviceCatalogue catalogue = new DeviceCatalogue();
        catalogue.setParentId("");
        catalogue.setCatalogueName("修改前的名称");
        catalogueService.addCatalogue(catalogue);
        
        // 修改名称
        catalogue.setCatalogueName("修改后的名称");
        boolean result = catalogueService.editCatalogue(catalogue);
        
        assertTrue(result, "修改目录失败");
        System.out.println("目录修改成功");
        
        // 验证修改
        DeviceCatalogue updated = catalogueService.getById(catalogue.getId());
        assertEquals("修改后的名称", updated.getCatalogueName(), "修改后的名称不匹配");
    }

    /**
     * 测试删除设备目录
     */
    @Test
    public void testDeleteCatalogue() {
        // 先添加一个目录
        DeviceCatalogue catalogue = new DeviceCatalogue();
        catalogue.setParentId("");
        catalogue.setCatalogueName("待删除的目录");
        catalogueService.addCatalogue(catalogue);
        
        // 删除目录
        boolean result = catalogueService.deleteCatalogue(catalogue.getId());
        assertTrue(result, "删除目录失败");
        System.out.println("目录删除成功，ID: " + catalogue.getId());
        
        // 验证删除
        DeviceCatalogue deleted = catalogueService.getById(catalogue.getId());
        assertNull(deleted, "目录未被删除");
    }
}
