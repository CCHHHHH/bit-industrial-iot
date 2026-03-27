package com.bit.iot.rule.model.enums;

import lombok.Getter;

/**
 * 时间窗口单位枚举
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
public enum WindowUnitEnum {

    SECOND("s", "秒",  1L),
    MINUTE("m", "分钟", 60L),
    HOUR("h",   "小时", 3600L),
    DAY("d",    "天",   86400L);

    private final String code;
    private final String description;
    /** 换算为秒数 */
    private final long seconds;

    WindowUnitEnum(String code, String description, long seconds) {
        this.code = code;
        this.description = description;
        this.seconds = seconds;
    }

    public static WindowUnitEnum getByCode(String code) {
        if (code == null) return null;
        for (WindowUnitEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return null;
    }

    /** 将窗口大小转换为毫秒 */
    public static long toMillis(long size, String unit) {
        WindowUnitEnum u = getByCode(unit);
        if (u == null) return size * 1000L;
        return size * u.seconds * 1000L;
    }
}
