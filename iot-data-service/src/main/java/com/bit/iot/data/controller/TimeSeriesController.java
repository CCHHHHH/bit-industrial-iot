package com.bit.iot.data.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.bit.iot.data.model.request.RawTimeSeriesQueryRequest;
import com.bit.iot.data.model.request.RuleWindowQueryRequest;
import com.bit.iot.data.model.vo.TimeSeriesPointVO;
import com.bit.iot.data.service.TimeSeriesQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/timeseries")
@Tag(name = "时序数据查询接口", description = "统一的时序数据查询网关")
public class TimeSeriesController extends BaseController {

    private final TimeSeriesQueryService timeSeriesQueryService;

    public TimeSeriesController(TimeSeriesQueryService timeSeriesQueryService) {
        this.timeSeriesQueryService = timeSeriesQueryService;
    }

    @PostMapping("/query/raw")
    @Operation(summary = "通用原始时序查询")
    public Result<List<TimeSeriesPointVO>> queryRaw(@RequestBody RawTimeSeriesQueryRequest request) {
        return success(timeSeriesQueryService.queryRaw(request));
    }

    @PostMapping("/query/rule-window")
    @Operation(summary = "按规则数据源批量取数")
    public Result<List<TimeSeriesPointVO>> queryRuleWindow(@RequestBody RuleWindowQueryRequest request) {
        return success(timeSeriesQueryService.queryRuleWindow(request));
    }

    @GetMapping("/health")
    @Operation(summary = "TDEngine 依赖健康检查")
    public Result<Map<String, Object>> health() {
        return success(timeSeriesQueryService.health());
    }
}
