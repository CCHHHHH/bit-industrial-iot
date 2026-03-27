package com.bit.iot.integration.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 映射类型枚举
 *
 * @author chenhao
 * @since 2026-03-20
 */
@Getter
public enum MappingTypeEnum {

    @Schema(description = "设备属性")
    DEVICE_PROPERTY("device_property", "设备属性", "handleDeviceProperty"),

    @Schema(description = "设备状态")
    DEVICE_STATUS("device_status", "设备状态", "handleDeviceStatus"),

    @Schema(description = "时序数据")
    TIME_SERIES_DATA("time_series_data", "时序数据", "handleTimeSeriesData");

    private final String code;
    private final String description;
    private final String pluginMethodName;

    MappingTypeEnum(String code, String description, String pluginMethodName) {
        this.code = code;
        this.description = description;
        this.pluginMethodName = pluginMethodName;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 编码
     * @return 枚举
     */
    public static MappingTypeEnum getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (MappingTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
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
}
