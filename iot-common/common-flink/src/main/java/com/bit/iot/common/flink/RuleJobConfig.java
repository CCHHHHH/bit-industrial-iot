package com.bit.iot.common.flink;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 规则 Job 配置（传递给 Flink Job 的完整运行参数）
 * <p>
 * 由 iot-rule-service 组装，JSON 序列化后 Base64 编码传入 Flink Job 的 main() 参数。
 * </p>
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Data
public class RuleJobConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- 规则基础 ----
    private String ruleId;
    private String ruleName;
    /** 触发类型：periodic / realtime */
    private String triggerType;

    // ---- 窗口配置 ----
    /** 窗口类型：tumbling / sliding / session */
    private String windowType;
    /** 窗口大小（毫秒） */
    private long windowSizeMs;
    /** 滑动步长（毫秒，仅 sliding 有效，0 则默认 windowSizeMs / 4） */
    private long windowSlideMs;

    // ---- 分组策略 ----
    /** 分组键策略：device_point（默认，按设备+测点）/ device（按设备） */
    private String keyStrategy = "device_point";

    // ---- 算法配置 ----
    /** 算法类型：jar / python */
    private String algorithmType;
    /** 算法文件路径（Flink 集群可访问的共享路径） */
    private String algorithmPath;
    /** JAR 入口类全限定名 */
    private String algorithmClass;
    /** 自定义参数 */
    private Map<String, String> ruleParams;

    // ---- 数据源配置（多设备多测点） ----
    private List<DataSourceConfig> dataSources;

    // ---- 并行度 ----
    private int parallelism = 2;

    // ---- 连接配置 ----
    private TDEngineConfig tdengineConfig;
    private MysqlConfig mysqlConfig;
    private MqttConfig mqttConfig;

    // ================================================================
    // 内部配置结构
    // ================================================================

    @Data
    public static class DataSourceConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private String deviceId;
        private String deviceName;
        /** 测点编码列表，空 = 全量 */
        private List<String> pointCodes;
        private String timeRangeStart;
        private String timeRangeEnd;
    }

    @Data
    public static class TDEngineConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        /** jdbc:TAOS-RS://host:6041/db */
        private String jdbcUrl;
        private String username;
        private String password;
        /** 超级表名 */
        private String superTable;
    }

    @Data
    public static class MysqlConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private String jdbcUrl;
        private String username;
        private String password;
    }

    @Data
    public static class MqttConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private String brokerUrl;
        private String topicPattern;
        private String username;
        private String password;
    }
}
