package com.bit.iot.integration.model.request;

import lombok.Data;

import com.bit.iot.integration.model.dto.PluginConfigItemDTO;

import java.util.ArrayList;
import java.util.List;

@Data
public class IntegrationPluginConfigRequest {
    private List<PluginConfigItemDTO> configItems = new ArrayList<>();
}
