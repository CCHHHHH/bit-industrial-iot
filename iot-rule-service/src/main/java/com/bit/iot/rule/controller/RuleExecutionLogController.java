package com.bit.iot.rule.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.rule.model.entity.RuleExecutionLog;
import com.bit.iot.rule.service.IRuleExecutionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则执行日志接口
 *
 * @author chenhao
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/rule-execution-log")
@Tag(name = "规则执行日志接口", description = "查询和清理规则执行历史记录")
public class RuleExecutionLogController extends BaseController {

    @Autowired
    private IRuleExecutionLogService executionLogService;

    @GetMapping("/list")
    @Operation(summary = "分页查询执行日志")
    public Result<List<RuleExecutionLog>> getExecutionLogList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            String ruleId,
            Integer execStatus) {
        Page<RuleExecutionLog> page = new Page<>(current, size);
        return success(executionLogService.getExecutionLogList(page, ruleId, execStatus));
    }

    @DeleteMapping("/clear/{ruleId}")
    @Operation(summary = "清空指定规则的执行日志")
    public Result<Void> clearLogs(@PathVariable String ruleId) {
        return executionLogService.clearLogsByRuleId(ruleId) ? success("清空成功") : error("清空失败");
    }
}
