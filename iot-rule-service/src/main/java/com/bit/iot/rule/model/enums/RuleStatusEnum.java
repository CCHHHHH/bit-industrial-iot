package com.bit.iot.rule.model.enums;

import lombok.Getter;

/**
 * 规则运行状态枚举
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
public enum RuleStatusEnum {

    STOPPED(0, "已停止"),
    RUNNING(1, "运行中");

    private final int code;
    private final String description;

    RuleStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
