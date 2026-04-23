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
 * 规则配置表
 * 定义一条完整的规则：绑定算法、时间窗口、触发方式
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("rule_config")
@Schema(description = "规则配置")
public class RuleConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /** 规则名称（唯一） */
    @Schema(description = "规则名称")
    @TableField("rule_name")
    private String ruleName;

    /** 规则描述 */
    @Schema(description = "规则描述")
    @TableField("rule_desc")
    private String ruleDesc;

    /** 绑定的算法 ID */
    @Schema(description = "算法 ID")
    @TableField("algorithm_id")
    private String algorithmId;

    /**
     * 触发类型：periodic（定时）/ realtime（实时流）
     */
    @Schema(description = "触发类型（periodic/realtime）")
    @TableField("trigger_type")
    private String triggerType;

    /** 定时触发的 Cron 表达式（trigger_type=periodic 时填写） */
    @Schema(description = "Cron 表达式")
    @TableField("trigger_cron")
    private String triggerCron;

    /**
     * 时间窗口类型：tumbling（滚动）/ sliding（滑动）/ session（会话）
     */
    @Schema(description = "时间窗口类型（tumbling/sliding/session）")
    @TableField("window_type")
    private String windowType;

    /** 窗口大小数值 */
    @Schema(description = "窗口大小")
    @TableField("window_size")
    private Long windowSize;

    /** 滑动窗口步长（仅 sliding 类型有效） */
    @Schema(description = "滑动窗口步长")
    @TableField("window_slide")
    private Long windowSlide;

    /** 窗口单位：s / m / h / d */
    @Schema(description = "窗口单位（s/m/h/d）")
    @TableField("window_unit")
    private String windowUnit;

    /**
     * 分组策略：device_point（默认，按设备+测点独立窗口）/ device（按设备聚合）
     */
    @Schema(description = "分组策略（device_point/device）")
    @TableField("key_strategy")
    private String keyStrategy;

    /** Flink Job 并行度 */
    @Schema(description = "Flink Job 并行度")
    @TableField("parallelism")
    private Integer parallelism;

    /** 规则状态：0-已停止，1-运行中，2-已完成，3-失败 */
    @Schema(description = "规则状态（0-已停止，1-运行中，2-已完成，3-失败）")
    @TableField("rule_status")
    private Integer ruleStatus;

    /** Flink Job ID（运行时由 FlinkJobManager 填充） */
    @Schema(description = "Flink Job ID")
    @TableField("flink_job_id")
    private String flinkJobId;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
