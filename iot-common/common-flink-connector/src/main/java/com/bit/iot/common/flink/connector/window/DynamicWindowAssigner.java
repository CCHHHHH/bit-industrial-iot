package com.bit.iot.common.flink.connector.window;

import com.bit.iot.common.flink.RuleJobConfig;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import java.time.Duration;

/**
 * 动态窗口分配器
 * <p>
 * 根据规则配置中的 windowType / windowSizeMs / windowSlideMs
 * 动态选择 Tumbling / Sliding / Session 窗口策略。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
public class DynamicWindowAssigner {

    /**
     * 根据配置对 KeyedStream 应用时间窗口
     *
     * @param keyedStream 已按 Key 分组的流
     * @param config      规则 Job 配置
     * @param <T>         数据类型
     * @return WindowedStream
     */
    public static <T> WindowedStream<T, String, TimeWindow> applyWindow(
            KeyedStream<T, String> keyedStream,
            RuleJobConfig config) {

        long windowSizeMs = config.getWindowSizeMs();
        String windowType = config.getWindowType();

        if (windowSizeMs <= 0) {
            windowSizeMs = 60_000L; // 默认 1 分钟
        }

        String normalizedType = windowType != null ? windowType.toLowerCase() : "tumbling";
        if ("sliding".equals(normalizedType)) {
            long slideMs = config.getWindowSlideMs() > 0
                    ? config.getWindowSlideMs()
                    : windowSizeMs / 4;
            return keyedStream.window(
                    SlidingEventTimeWindows.of(
                            Duration.ofMillis(windowSizeMs),
                            Duration.ofMillis(slideMs)));
        }
        if ("session".equals(normalizedType)) {
            return keyedStream.window(
                    EventTimeSessionWindows.withGap(Duration.ofMillis(windowSizeMs)));
        }
        return keyedStream.window(
                TumblingEventTimeWindows.of(Duration.ofMillis(windowSizeMs)));
    }
}
