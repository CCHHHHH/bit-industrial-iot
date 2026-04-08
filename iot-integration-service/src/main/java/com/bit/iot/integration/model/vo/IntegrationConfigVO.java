package com.bit.iot.integration.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class IntegrationConfigVO {
    private String id;
    private String integrationName;
    private String pluginId;
    private Integer integrationStatus;
    private String integrationDesc;
    private Date createTime;
    private Date updateTime;
}
