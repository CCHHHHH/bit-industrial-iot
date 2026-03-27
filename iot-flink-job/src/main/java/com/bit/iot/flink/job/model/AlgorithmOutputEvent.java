package com.bit.iot.flink.job.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 算法输出事件（窗口计算结果）
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Data
public class AlgorithmOutputEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则 ID */
    private String ruleId;

    /** 分组键（deviceId 或 deviceId#pointCode） */
    private String key;

    /** 窗口起始时间（毫秒） */
    private long windowStart;

    /** 窗口结束时间（毫秒） */
    private long windowEnd;

    /** 算法是否成功 */
    private boolean success;

    /** 算法输出数据 */
    private Map<String, Object> resultData;

    /** 错误信息 */
    private String errorMsg;

    /** 算法执行耗时（毫秒） */
    private long durationMs;

    /** 处理时间（毫秒） */
    private long processTime;
}
