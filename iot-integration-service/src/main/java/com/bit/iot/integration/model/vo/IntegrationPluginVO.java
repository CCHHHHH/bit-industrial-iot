package com.bit.iot.integration.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class IntegrationPluginVO {
    private String id;
    private String pluginName;
    private String pluginDescription;
    private String pluginPath;
    private String pluginType;
    private Integer pluginStatus;
    private String pluginVersion;
    private Integer pluginSize;
    private Date createTime;
    private Date updateTime;
}
