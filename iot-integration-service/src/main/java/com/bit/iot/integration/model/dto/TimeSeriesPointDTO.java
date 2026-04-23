package com.bit.iot.integration.model.dto;

import java.time.Instant;

public record TimeSeriesPointDTO(
        String deviceId,
        String pointCode,
        Instant timestamp,
        Double value,
        Integer quality
) {
}
