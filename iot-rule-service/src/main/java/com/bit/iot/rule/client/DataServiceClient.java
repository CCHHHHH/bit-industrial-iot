package com.bit.iot.rule.client;

import bit.iot.common.controller.BusinessException;
import bit.iot.common.controller.Result;
import com.bit.iot.common.flink.DataPoint;
import com.bit.iot.rule.client.model.RuleDataSourceQuery;
import com.bit.iot.rule.client.model.RuleWindowQueryRequest;
import com.bit.iot.rule.client.model.TimeSeriesPointResponse;
import com.bit.iot.rule.model.entity.RuleDataSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${data.service.base-url:http://127.0.0.1:9005/iot}")
    private String dataServiceBaseUrl;

    public List<DataPoint> queryRuleWindow(List<RuleDataSource> dataSources,
                                           long queryStartTime,
                                           long queryEndTime,
                                           int limitPerSource) {
        RuleWindowQueryRequest request = new RuleWindowQueryRequest();
        request.setQueryStartTime(queryStartTime);
        request.setQueryEndTime(queryEndTime);
        request.setLimitPerSource(limitPerSource);
        request.setDataSources(dataSources.stream().map(this::toQuery).collect(Collectors.toList()));

        ResponseEntity<Result<List<TimeSeriesPointResponse>>> response = restTemplate.exchange(
                dataServiceBaseUrl + "/timeseries/query/rule-window",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {
                }
        );

        Result<List<TimeSeriesPointResponse>> body = response.getBody();
        if (body == null) {
            throw new BusinessException("iot-data-service 返回为空");
        }
        if (body.getCode() == null || body.getCode() != 200) {
            throw new BusinessException("iot-data-service 查询失败: " + body.getMessage());
        }
        List<TimeSeriesPointResponse> points = body.getData();
        if (points == null) {
            return Collections.emptyList();
        }
        return points.stream().map(TimeSeriesPointResponse::toDataPoint).toList();
    }

    private RuleDataSourceQuery toQuery(RuleDataSource source) {
        RuleDataSourceQuery query = new RuleDataSourceQuery();
        query.setDeviceId(source.getDeviceId());
        query.setDeviceName(source.getDeviceName());
        query.setTimeRangeStart(source.getTimeRangeStart());
        query.setTimeRangeEnd(source.getTimeRangeEnd());
        query.setPointCodes(parsePointCodes(source.getPointCodes()));
        return query;
    }

    private List<String> parsePointCodes(String pointCodes) {
        if (pointCodes == null || pointCodes.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(pointCodes, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return List.of(pointCodes);
        }
    }
}
