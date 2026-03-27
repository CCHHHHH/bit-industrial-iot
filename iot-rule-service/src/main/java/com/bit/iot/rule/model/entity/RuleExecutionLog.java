package com.bit.iot.rule.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 规则执行日志表
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("rule_execution_log")
@Schema(description = "规则执行日志")
public class RuleExecutionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /** 所属规则 ID */
    @Schema(description = "规则 ID")
    @TableField("rule_id")
    private String ruleId;

    /** 执行开始时间 */
    @Schema(description = "执行开始时间")
    @TableField("start_time")
    private Date startTime;

    /** 执行结束时间 */
    @Schema(description = "执行结束时间")
    @TableField("end_time")
    private Date endTime;

    /** 执行状态：0-执行中，1-成功，2-失败 */
    @Schema(description = "执行状态（0-执行中，1-成功，2-失败）")
    @TableField("exec_status")
    private Integer execStatus;

    /** 算法输出结果（JSON） */
    @Schema(description = "执行结果（JSON）")
    @TableField("result_data")
    private String resultData;

    /** 错误信息 */
    @Schema(description = "错误信息")
    @TableField("error_msg")
    private String errorMsg;

    /** 执行耗时（毫秒） */
    @Schema(description = "执行耗时（ms）")
    @TableField("duration_ms")
    private Long durationMs;
}
