package com.bit.iot.system.model.vo;

import lombok.Data;

@Data
public class PermissionVO {
    private String id;
    private String permissionName;
    private String permissionCode;
    private String permissionDesc;
    private String permissionType;
}
