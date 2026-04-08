package com.bit.iot.integration.model.vo;

import lombok.Data;

import com.bit.iot.integration.model.dto.PluginConfigItemDTO;

import java.util.ArrayList;
import java.util.List;

@Data
public class IntegrationPluginConfigVO {
    private String pluginId;
    private String integrationId;
    private List<PluginConfigItemDTO> configItems = new ArrayList<>();
}
