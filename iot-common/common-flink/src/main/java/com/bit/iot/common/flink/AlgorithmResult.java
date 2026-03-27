package com.bit.iot.common.flink;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 算法执行结果
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Data
public class AlgorithmResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;

    /** 结果数据（key-value，最终序列化为 JSON） */
    private Map<String, Object> data;

    /** 错误信息（失败时填写） */
    private String errorMsg;

    public static AlgorithmResult success(Map<String, Object> data) {
        AlgorithmResult r = new AlgorithmResult();
        r.setSuccess(true);
        r.setData(data);
        return r;
    }

    public static AlgorithmResult failure(String errorMsg) {
        AlgorithmResult r = new AlgorithmResult();
        r.setSuccess(false);
        r.setErrorMsg(errorMsg);
        return r;
    }
}
