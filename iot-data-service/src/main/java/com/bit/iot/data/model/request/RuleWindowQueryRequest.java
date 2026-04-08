package com.bit.iot.data.model.request;

import lombok.Data;

import java.util.List;

@Data
public class RuleWindowQueryRequest {

    private Long queryStartTime;
    private Long queryEndTime;
    private Integer limitPerSource;
    private List<RuleDataSourceQuery> dataSources;
}
