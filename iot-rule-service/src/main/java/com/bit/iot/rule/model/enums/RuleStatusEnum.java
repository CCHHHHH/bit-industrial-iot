package com.bit.iot.rule.model.enums;

/**
 * 规则运行状态
 *
 * @author chenhao
 * @since 2026-04-10
 */
public enum RuleStatusEnum {

    STOPPED(0),
    RUNNING(1),
    COMPLETED(2),
    FAILED(3);

    private final int code;

    RuleStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
