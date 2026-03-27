package com.bit.iot.rule.flink;

/**
 * Flink Job 状态枚举
 * 对应 Flink REST API 返回的 Job 状态
 *
 * @author chenhao
 * @since 2026-03-27
 */
public enum FlinkJobStatus {

    CREATED,
    RUNNING,
    FAILING,
    FAILED,
    CANCELLING,
    CANCELED,
    FINISHED,
    RESTARTING,
    SUSPENDED,
    RECONCILING,
    /** 未在 Flink 集群中找到对应 Job */
    NOT_FOUND,
    /** 查询失败 */
    UNKNOWN;

    /**
     * 安全解析，未知状态返回 UNKNOWN
     */
    public static FlinkJobStatus safeValueOf(String state) {
        if (state == null) return UNKNOWN;
        try {
            return valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
