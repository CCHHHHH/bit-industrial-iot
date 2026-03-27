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
 * 规则数据源表
 * 配置规则读取哪些设备的哪些测点数据，以及时段范围
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("rule_data_source")
@Schema(description = "规则数据源")
public class RuleDataSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /** 所属规则 ID */
    @Schema(description = "规则 ID")
    @TableField("rule_id")
    private String ruleId;

    /** 设备 ID */
    @Schema(description = "设备 ID")
    @TableField("device_id")
    private String deviceId;

    /** 设备名称（冗余，便于展示） */
    @Schema(description = "设备名称")
    @TableField("device_name")
    private String deviceName;

    /**
     * 测点编码列表（JSON 数组）
     * 例：["P001","P002","T001"]，为空则读取该设备全部测点
     */
    @Schema(description = "测点编码列表（JSON 数组，空则全量）")
    @TableField("point_codes")
    private String pointCodes;

    /**
     * 数据时段起始（格式：HH:mm:ss 或 yyyy-MM-dd HH:mm:ss）
     * 为空则不限制起始时间
     */
    @Schema(description = "数据时段起始")
    @TableField("time_range_start")
    private String timeRangeStart;

    /**
     * 数据时段结束（格式同上）
     * 为空则使用当前时间
     */
    @Schema(description = "数据时段结束")
    @TableField("time_range_end")
    private String timeRangeEnd;

    @TableField("create_time")
    private Date createTime;
}
