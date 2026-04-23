package com.bit.iot.rule.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 规则配置列表项 DTO（平铺字段）
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Data
@Schema(description = "规则配置列表项")
public class RuleConfigListItemDTO {

    @Schema(description = "规则 ID")
    private String id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则描述")
    private String ruleDesc;

    @Schema(description = "算法 ID")
    private String algorithmId;

    @Schema(description = "算法名称")
    private String algorithmName;

    @Schema(description = "算法类型（jar/python）")
    private String algorithmType;

    @Schema(description = "触发类型（periodic/realtime）")
    private String triggerType;

    @Schema(description = "Cron 表达式")
    private String triggerCron;

    @Schema(description = "时间窗口类型")
    private String windowType;

    @Schema(description = "窗口大小")
    private Long windowSize;

    @Schema(description = "窗口单位")
    private String windowUnit;

    @Schema(description = "分组策略")
    private String keyStrategy;

    @Schema(description = "并行度")
    private Integer parallelism;

    @Schema(description = "规则状态（0-已停止，1-运行中，2-已完成，3-失败）")
    private Integer ruleStatus;

    @Schema(description = "Flink Job ID")
    private String flinkJobId;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
