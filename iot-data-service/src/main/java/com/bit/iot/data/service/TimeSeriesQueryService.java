package com.bit.iot.data.service;

import com.bit.iot.data.model.request.RawTimeSeriesQueryRequest;
import com.bit.iot.data.model.request.RuleWindowQueryRequest;
import com.bit.iot.data.model.vo.TimeSeriesPointVO;

import java.util.List;
import java.util.Map;

public interface TimeSeriesQueryService {

    List<TimeSeriesPointVO> queryRaw(RawTimeSeriesQueryRequest request);

    List<TimeSeriesPointVO> queryRuleWindow(RuleWindowQueryRequest request);

    Map<String, Object> health();
}
