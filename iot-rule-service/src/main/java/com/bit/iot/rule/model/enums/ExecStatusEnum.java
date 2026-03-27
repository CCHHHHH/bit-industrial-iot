package com.bit.iot.rule.model.enums;

import lombok.Getter;

/**
 * 规则执行状态枚举
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
public enum ExecStatusEnum {

    RUNNING(0, "执行中"),
    SUCCESS(1, "执行成功"),
    FAILED(2,  "执行失败");

    private final int code;
    private final String description;

    ExecStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
