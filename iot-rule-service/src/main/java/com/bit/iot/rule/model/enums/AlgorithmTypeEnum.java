package com.bit.iot.rule.model.enums;

import lombok.Getter;

/**
 * 算法文件类型枚举
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
public enum AlgorithmTypeEnum {

    JAR("jar", "Java JAR 包"),
    PYTHON("python", "Python 脚本");

    private final String code;
    private final String description;

    AlgorithmTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AlgorithmTypeEnum getByCode(String code) {
        if (code == null) return null;
        for (AlgorithmTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return null;
    }
}
