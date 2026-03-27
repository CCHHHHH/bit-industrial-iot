package com.bit.iot.rule.model.enums;

import lombok.Getter;

/**
 * 时间窗口类型枚举
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
public enum WindowTypeEnum {

    TUMBLING("tumbling", "滚动窗口"),
    SLIDING("sliding",   "滑动窗口"),
    SESSION("session",   "会话窗口");

    private final String code;
    private final String description;

    WindowTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static WindowTypeEnum getByCode(String code) {
        if (code == null) return null;
        for (WindowTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return null;
    }
}
