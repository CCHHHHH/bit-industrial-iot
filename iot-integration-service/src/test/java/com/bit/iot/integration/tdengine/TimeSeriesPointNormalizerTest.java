package com.bit.iot.integration.tdengine;

import com.bit.iot.integration.model.dto.TimeSeriesPointDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSeriesPointNormalizerTest {

    private final TimeSeriesPointNormalizer normalizer = new TimeSeriesPointNormalizer(new ObjectMapper());

    @Test
    void normalizeStandardPointJson() {
        List<TimeSeriesPointDTO> points = normalizer.normalize("""
                {
                  "deviceId": "device-001",
                  "pointCode": "TEMP_001",
                  "timestamp": 1712120400000,
                  "value": 25.5,
                  "quality": 0
                }
                """, Instant.parse("2026-04-01T00:00:00Z"));

        assertThat(points).hasSize(1);
        TimeSeriesPointDTO point = points.getFirst();
        assertThat(point.deviceId()).isEqualTo("device-001");
        assertThat(point.pointCode()).isEqualTo("TEMP_001");
        assertThat(point.timestamp()).isEqualTo(Instant.ofEpochMilli(1712120400000L));
        assertThat(point.value()).isEqualTo(25.5);
        assertThat(point.quality()).isZero();
    }

    @Test
    void normalizeSimulatorPayload() {
        List<TimeSeriesPointDTO> points = normalizer.normalize(Map.of(
                "deviceId", "device-001",
                "timestamp", 1712120400000L,
                "values", Map.of(
                        "TEMP_001", 25.5,
                        "PRESS_001", 0.82
                )
        ), Instant.parse("2026-04-01T00:00:00Z"));

        assertThat(points).hasSize(2);
        assertThat(points)
                .extracting(TimeSeriesPointDTO::pointCode)
                .containsExactlyInAnyOrder("TEMP_001", "PRESS_001");
    }

    @Test
    void normalizeArrayWithFallbackTimestampAndQuality() {
        Instant fallback = Instant.parse("2026-04-01T00:00:00Z");

        List<TimeSeriesPointDTO> points = normalizer.normalize(List.of(
                Map.of("deviceId", "device-001", "pointCode", "TEMP_001", "value", "25.5")
        ), fallback);

        assertThat(points).hasSize(1);
        assertThat(points.getFirst().timestamp()).isEqualTo(fallback);
        assertThat(points.getFirst().quality()).isZero();
    }

    @Test
    void rejectInvalidPoint() {
        assertThatThrownBy(() -> normalizer.normalize(Map.of(
                "deviceId", "device-001",
                "pointCode", "TEMP_001"
        ), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
    }
}
