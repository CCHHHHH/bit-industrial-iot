package com.bit.iot.device.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.device.model.entity.DeviceCatalogue;
import com.bit.iot.device.model.request.DeviceCatalogueRequest;
import com.bit.iot.device.model.vo.DeviceCatalogueVO;
import com.bit.iot.device.service.IDeviceCatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 设备目录 前端控制器
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@RestController
@RequestMapping("/device-catalogue")
@Tag(name = "设备目录管理接口", description = "设备目录相关操作接口")
public class DeviceCatalogueController extends BaseController {

    @Autowired
    private IDeviceCatalogueService catalogueService;

    @GetMapping("/list")
    @Operation(summary = "分页查询设备目录列表")
    public Result<List<DeviceCatalogueVO>> getCatalogueList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String catalogueName) {
        Page<DeviceCatalogue> page = new Page<>(current, size);
        Page<DeviceCatalogue> result = catalogueService.getCatalogueList(page, catalogueName);
        Page<DeviceCatalogueVO> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    @PostMapping
    @Operation(summary = "新增设备目录")
    public Result<Void> addCatalogue(@RequestBody DeviceCatalogueRequest catalogue) {
        DeviceCatalogue entity = new DeviceCatalogue();
        BeanUtils.copyProperties(catalogue, entity);
        boolean success = catalogueService.addCatalogue(entity);
        return success ? success("新增成功") : error("新增失败");
    }

    @PutMapping
    @Operation(summary = "编辑设备目录")
    public Result<Void> editCatalogue(@RequestBody DeviceCatalogueRequest catalogue) {
        DeviceCatalogue entity = new DeviceCatalogue();
        BeanUtils.copyProperties(catalogue, entity);
        boolean success = catalogueService.editCatalogue(entity);
        return success ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除设备目录")
    public Result<Void> deleteCatalogue(@PathVariable String id) {
        boolean success = catalogueService.deleteCatalogue(id);
        return success ? success("删除成功") : error("删除失败");
    }

    @GetMapping("/tree")
    @Operation(summary = "查询树形结构的设备目录")
    public Result<List<DeviceCatalogueVO>> getTreeCatalogues() {
        List<DeviceCatalogue> treeCatalogues = catalogueService.getTreeCatalogues();
        return success(treeCatalogues.stream().map(this::toVO).toList());
    }

    private DeviceCatalogueVO toVO(DeviceCatalogue catalogue) {
        DeviceCatalogueVO vo = new DeviceCatalogueVO();
        BeanUtils.copyProperties(catalogue, vo);
        if (catalogue.getChildren() != null) {
            vo.setChildren(catalogue.getChildren().stream().map(this::toVO).toList());
        }
        return vo;
    }
}
