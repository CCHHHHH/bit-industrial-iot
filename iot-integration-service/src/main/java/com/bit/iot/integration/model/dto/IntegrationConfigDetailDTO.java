package com.bit.iot.integration.model.dto;

import com.bit.iot.integration.model.vo.IntegrationConfigParamVO;
import com.bit.iot.integration.model.vo.IntegrationConfigVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 集成配置详情 DTO
 * 
 * @author chenhao
 * @date 2026/3/20
 */
@Getter
@Setter
@Schema(description = "集成配置详情（包含配置参数）")
public class IntegrationConfigDetailDTO {

    /**
     * 集成配置信息
     */
    @Schema(description = "集成配置信息")
    private IntegrationConfigVO config;

    /**
     * 配置参数列表
     */
    @Schema(description = "配置参数列表")
    private List<IntegrationConfigParamVO> configParams;
}
