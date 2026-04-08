package com.bit.iot.device.model.request;

import lombok.Data;

@Data
public class DeviceCatalogueRequest {
    private String id;
    private String parentId;
    private String catalogueName;
}
