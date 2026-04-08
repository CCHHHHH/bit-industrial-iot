package com.bit.iot.data.service.impl;

import com.bit.iot.data.config.TDEngineProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

class TimeSeriesQueryServiceImplTest {

    @Test
    void shouldResolveEpochString() {
        TimeSeriesQueryServiceImpl service = new TimeSeriesQueryServiceImpl(new TDEngineProperties());
        long resolved = service.resolveSourceBoundary("1711526400000", 0L, 0L, true);
        Assertions.assertEquals(1711526400000L, resolved);
    }

    @Test
    void shouldResolveClockTimeAgainstFallbackDate() {
        TimeSeriesQueryServiceImpl service = new TimeSeriesQueryServiceImpl(new TDEngineProperties());
        long fallbackEnd = LocalDateTime.of(2026, 4, 3, 10, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        long resolved = service.resolveSourceBoundary("08:30:00", 0L, fallbackEnd, true);
        long expected = LocalDateTime.of(2026, 4, 3, 8, 30, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        Assertions.assertEquals(expected, resolved);
    }
}
