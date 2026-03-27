package com.bit.iot.rule.model.enums;

import lombok.Getter;

/**
 * 规则触发类型枚举
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
public enum TriggerTypeEnum {

    PERIODIC("periodic", "定时触发（Cron）"),
    REALTIME("realtime", "实时流处理");

    private final String code;
    private final String description;

    TriggerTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TriggerTypeEnum getByCode(String code) {
        if (code == null) return null;
        for (TriggerTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return null;
    }
}
