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

/**
 * 规则参数表
 * 向算法传递的自定义 key-value 参数
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("rule_param")
@Schema(description = "规则参数")
public class RuleParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /** 所属规则 ID */
    @Schema(description = "规则 ID")
    @TableField("rule_id")
    private String ruleId;

    /** 参数键 */
    @Schema(description = "参数键")
    @TableField("param_key")
    private String paramKey;

    /** 参数值 */
    @Schema(description = "参数值")
    @TableField("param_value")
    private String paramValue;

    /** 参数说明 */
    @Schema(description = "参数说明")
    @TableField("param_desc")
    private String paramDesc;
}
