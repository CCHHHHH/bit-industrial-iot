package com.bit.iot.device.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DeviceCatalogueVO {
    private String id;
    private String parentId;
    private String catalogueName;
    private Date createTime;
    private Date updateTime;
    private List<DeviceCatalogueVO> children;
    private Long deviceCount;
}
