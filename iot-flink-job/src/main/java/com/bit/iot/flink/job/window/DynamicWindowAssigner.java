package com.bit.iot.flink.job.window;

import com.bit.iot.common.flink.RuleJobConfig;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

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

        return switch (windowType != null ? windowType.toLowerCase() : "tumbling") {
            case "sliding" -> {
                long slideMs = config.getWindowSlideMs() > 0
                        ? config.getWindowSlideMs()
                        : windowSizeMs / 4;
                yield keyedStream.window(
                        SlidingEventTimeWindows.of(
                                Time.milliseconds(windowSizeMs),
                                Time.milliseconds(slideMs)));
            }
            case "session" -> keyedStream.window(
                    EventTimeSessionWindows.withGap(Time.milliseconds(windowSizeMs)));

            default -> // tumbling
                    keyedStream.window(
                            TumblingEventTimeWindows.of(Time.milliseconds(windowSizeMs)));
        };
    }
}
