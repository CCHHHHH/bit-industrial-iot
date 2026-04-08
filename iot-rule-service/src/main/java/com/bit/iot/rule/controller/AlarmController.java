package com.bit.iot.rule.controller;

import bit.iot.common.controller.BaseController;
import bit.iot.common.controller.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.iot.rule.model.entity.AlarmRecord;
import com.bit.iot.rule.model.request.AlarmQueryRequest;
import com.bit.iot.rule.model.vo.AlarmRecordVO;
import com.bit.iot.rule.service.IAlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警管理接口。
 */
@RestController
@RequestMapping("/alarm")
@Tag(name = "告警管理接口", description = "规则告警的分页查询、详情和处理")
public class AlarmController extends BaseController {

    @Autowired
    private IAlarmService alarmService;

    @GetMapping("/list")
    @Operation(summary = "分页查询告警")
    public Result<List<AlarmRecordVO>> getAlarmList(AlarmQueryRequest request) {
        long current = request != null && request.getCurrent() != null ? request.getCurrent() : 1L;
        long size = request != null && request.getSize() != null ? request.getSize() : 10L;
        Page<AlarmRecord> page = new Page<>(current, size);
        Page<AlarmRecord> result = alarmService.getAlarmList(page, request);
        Page<AlarmRecordVO> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return success(responsePage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询告警详情")
    public Result<AlarmRecordVO> getAlarmDetail(@PathVariable String id) {
        AlarmRecord alarmRecord = alarmService.getById(id);
        return alarmRecord == null ? error("告警不存在") : success(toVO(alarmRecord));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "处理告警，置为已解决")
    public Result<Void> resolveAlarm(@PathVariable String id) {
        AlarmRecord alarmRecord = alarmService.getById(id);
        if (alarmRecord == null) {
            return error("告警不存在");
        }
        return alarmService.resolveAlarm(id) ? success("处理成功") : error("处理失败");
    }

    private AlarmRecordVO toVO(AlarmRecord record) {
        AlarmRecordVO vo = new AlarmRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
