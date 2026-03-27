package com.bit.iot.integration.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 调度单位枚举
 *
 * @author chenhao
 * @since 2026-03-20
 */
@Getter
public enum SchedulerUnitEnum {

    @Schema(description = "天")
    DAY("d", "天", 86400L),

    @Schema(description = "小时")
    HOUR("h", "小时", 3600L),

    @Schema(description = "分钟")
    MINUTE("m", "分钟", 60L),

    @Schema(description = "秒")
    SECOND("s", "秒", 1L);

    private final String code;
    private final String description;
    private final Long seconds;

    SchedulerUnitEnum(String code, String description, Long seconds) {
        this.code = code;
        this.description = description;
        this.seconds = seconds;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 编码
     * @return 枚举
     */
    public static SchedulerUnitEnum getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SchedulerUnitEnum unit : values()) {
            if (unit.getCode().equals(code)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * 判断 code 是否有效
     *
     * @param code 编码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }

    /**
     * 将周期值转换为秒
     *
     * @param time 周期值
     * @param unitCode 单位编码
     * @return 秒数，如果单位无效返回 null
     */
    public static Long convertToSeconds(Long time, String unitCode) {
        if (time == null || unitCode == null) {
            return null;
        }
        SchedulerUnitEnum unit = getByCode(unitCode);
        if (unit == null) {
            return null;
        }
        return time * unit.getSeconds();
    }

    /**
     * 将秒数转换为指定单位的值
     *
     * @param seconds 秒数
     * @param unitCode 目标单位编码
     * @return 转换后的值，如果单位无效返回 null
     */
    public static Double convertFromSeconds(Long seconds, String unitCode) {
        if (seconds == null || unitCode == null) {
            return null;
        }
        SchedulerUnitEnum unit = getByCode(unitCode);
        if (unit == null) {
            return null;
        }
        return seconds.doubleValue() / unit.getSeconds();
    }
}
