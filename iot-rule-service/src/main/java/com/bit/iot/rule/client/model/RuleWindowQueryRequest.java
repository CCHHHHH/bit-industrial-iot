package com.bit.iot.rule.client.model;

import lombok.Data;

import java.util.List;

@Data
public class RuleWindowQueryRequest {

    private Long queryStartTime;
    private Long queryEndTime;
    private Integer limitPerSource;
    private List<RuleDataSourceQuery> dataSources;
}
