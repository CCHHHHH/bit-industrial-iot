package com.bit.iot.rule.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.rule.model.entity.RuleAlgorithm;
import com.bit.iot.rule.model.request.RuleAlgorithmRequest;
import com.bit.iot.rule.model.vo.RuleAlgorithmVO;
import com.bit.iot.rule.service.IRuleAlgorithmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 规则算法管理接口
 *
 * @author chenhao
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/rule-algorithm")
@Tag(name = "规则算法管理接口", description = "管理 JAR 包和 Python 脚本算法")
public class RuleAlgorithmController extends BaseController {

    @Autowired
    private IRuleAlgorithmService algorithmService;

    @GetMapping("/list")
    @Operation(summary = "分页查询算法列表")
    public Result<List<RuleAlgorithmVO>> getAlgorithmList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String algorithmName,
            String algorithmType) {
        Page<RuleAlgorithm> page = new Page<>(current, size);
        Page<RuleAlgorithm> result = algorithmService.getAlgorithmList(page, algorithmName, algorithmType);
        Page<RuleAlgorithmVO> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传算法文件（JAR 或 Python）")
    public Result<RuleAlgorithmVO> uploadAlgorithm(
            @Parameter(description = "算法文件", required = true)
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String algorithmName,
            @RequestParam(required = false, defaultValue = "") String algorithmDesc,
            @RequestParam(required = false) String algorithmType,
            @Parameter(description = "入口类全限定名（JAR 必填）")
            @RequestParam(required = false) String algorithmClass,
            @RequestParam(required = false, defaultValue = "1.0.0") String algorithmVersion) {
        try {
            RuleAlgorithm algorithm = algorithmService.uploadAlgorithm(
                    file, algorithmName, algorithmDesc, algorithmType, algorithmClass, algorithmVersion);
            return success(toVO(algorithm));
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "新增算法（仅元数据）")
    public Result<Void> addAlgorithm(@RequestBody RuleAlgorithmRequest algorithm) {
        RuleAlgorithm entity = new RuleAlgorithm();
        BeanUtils.copyProperties(algorithm, entity);
        try {
            return algorithmService.addAlgorithm(entity) ? success("新增成功") : error("新增失败");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "编辑算法")
    public Result<Void> editAlgorithm(@RequestBody RuleAlgorithmRequest algorithm) {
        RuleAlgorithm entity = new RuleAlgorithm();
        BeanUtils.copyProperties(algorithm, entity);
        return algorithmService.editAlgorithm(entity) ? success("修改成功") : error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除算法（同时删除文件）")
    public Result<Void> deleteAlgorithm(@PathVariable String id) {
        return algorithmService.deleteAlgorithm(id) ? success("删除成功") : error("删除失败");
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用算法")
    public Result<Void> enableAlgorithm(@PathVariable String id) {
        try {
            return algorithmService.enableAlgorithm(id) ? success("启用成功") : error("启用失败");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用算法")
    public Result<Void> disableAlgorithm(@PathVariable String id) {
        try {
            return algorithmService.disableAlgorithm(id) ? success("禁用成功") : error("禁用失败");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    private RuleAlgorithmVO toVO(RuleAlgorithm algorithm) {
        RuleAlgorithmVO vo = new RuleAlgorithmVO();
        BeanUtils.copyProperties(algorithm, vo);
        return vo;
    }
}
