package com.bit.iot.system.model.request;

import lombok.Data;

@Data
public class PermissionRequest {
    private String id;
    private String permissionName;
    private String permissionCode;
    private String permissionDesc;
    private String permissionType;
}
