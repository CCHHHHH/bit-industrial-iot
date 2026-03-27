package com.bit.iot.rule.model.dto;

import com.bit.iot.rule.model.entity.RuleConfig;
import com.bit.iot.rule.model.entity.RuleDataSource;
import com.bit.iot.rule.model.entity.RuleParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 规则配置详情 DTO
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Data
@Schema(description = "规则配置详情")
public class RuleConfigDetailDTO {

    @Schema(description = "规则基本配置")
    private RuleConfig ruleConfig;

    @Schema(description = "算法名称")
    private String algorithmName;

    @Schema(description = "算法类型")
    private String algorithmType;

    @Schema(description = "数据源配置列表")
    private List<RuleDataSource> dataSources;

    @Schema(description = "规则参数列表")
    private List<RuleParam> params;
}
