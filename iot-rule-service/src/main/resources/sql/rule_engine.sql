-- ============================================================
-- 规则引擎模块建表 SQL（含 Flink 集成字段）
-- 数据库：bit_iot (MySQL)
-- ============================================================

USE bit_iot;

-- ------------------------------------------------------------
-- 1. 算法表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rule_algorithm` (
    `id`                VARCHAR(32)  NOT NULL COMMENT '主键',
    `algorithm_name`    VARCHAR(100) NOT NULL COMMENT '算法名称（唯一）',
    `algorithm_desc`    VARCHAR(500)          COMMENT '算法描述',
    `algorithm_type`    VARCHAR(10)  NOT NULL COMMENT '算法类型：jar / python',
    `algorithm_path`    VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `algorithm_class`   VARCHAR(200)          COMMENT '入口类全限定名（JAR 必填）',
    `algorithm_version` VARCHAR(20)           COMMENT '算法版本',
    `algorithm_status`  TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `file_size`         BIGINT                COMMENT '文件大小（字节）',
    `create_time`       DATETIME              COMMENT '创建时间',
    `update_time`       DATETIME              COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_algorithm_name` (`algorithm_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则算法表';

-- ------------------------------------------------------------
-- 2. 规则配置表（含 Flink 新字段）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rule_config` (
    `id`            VARCHAR(32)  NOT NULL COMMENT '主键',
    `rule_name`     VARCHAR(100) NOT NULL COMMENT '规则名称（唯一）',
    `rule_desc`     VARCHAR(500)          COMMENT '规则描述',
    `algorithm_id`  VARCHAR(32)           COMMENT '绑定的算法 ID',
    `trigger_type`  VARCHAR(20)           COMMENT '触发类型：periodic / realtime',
    `trigger_cron`  VARCHAR(100)          COMMENT 'Cron 表达式（periodic 时填写）',
    `window_type`   VARCHAR(20)           COMMENT '时间窗口类型：tumbling / sliding / session',
    `window_size`   BIGINT                COMMENT '窗口大小',
    `window_slide`  BIGINT                COMMENT '滑动窗口步长（仅 sliding 有效）',
    `window_unit`   VARCHAR(10)           COMMENT '窗口单位：s / m / h / d',
    `key_strategy`  VARCHAR(20)  DEFAULT 'device_point' COMMENT '分组策略：device_point / device',
    `parallelism`   INT          DEFAULT 2 COMMENT 'Flink Job 并行度',
    `rule_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '规则状态：0-停止，1-运行中',
    `flink_job_id`  VARCHAR(64)           COMMENT 'Flink Job ID（运行时填充）',
    `create_time`   DATETIME              COMMENT '创建时间',
    `update_time`   DATETIME              COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_name` (`rule_name`),
    KEY `idx_algorithm_id` (`algorithm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则配置表';

-- ------------------------------------------------------------
-- 3. 规则数据源表（多设备多测点）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rule_data_source` (
    `id`               VARCHAR(32)  NOT NULL COMMENT '主键',
    `rule_id`          VARCHAR(32)  NOT NULL COMMENT '所属规则 ID',
    `device_id`        VARCHAR(32)  NOT NULL COMMENT '设备 ID',
    `device_name`      VARCHAR(100)          COMMENT '设备名称',
    `point_codes`      TEXT                  COMMENT '测点编码列表（JSON 数组，空则全量）',
    `time_range_start` VARCHAR(30)           COMMENT '数据时段起始',
    `time_range_end`   VARCHAR(30)           COMMENT '数据时段结束',
    `create_time`      DATETIME              COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则数据源表';

-- ------------------------------------------------------------
-- 4. 规则参数表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rule_param` (
    `id`          VARCHAR(32)  NOT NULL COMMENT '主键',
    `rule_id`     VARCHAR(32)  NOT NULL COMMENT '所属规则 ID',
    `param_key`   VARCHAR(100) NOT NULL COMMENT '参数键',
    `param_value` VARCHAR(500)          COMMENT '参数值',
    `param_desc`  VARCHAR(200)          COMMENT '参数说明',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则参数表';

-- ------------------------------------------------------------
-- 5. 规则执行日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rule_execution_log` (
    `id`          VARCHAR(32) NOT NULL COMMENT '主键',
    `rule_id`     VARCHAR(32) NOT NULL COMMENT '所属规则 ID',
    `window_key`  VARCHAR(100)         COMMENT '窗口键（deviceId 或 deviceId#pointCode）',
    `start_time`  DATETIME             COMMENT '执行开始时间',
    `end_time`    DATETIME             COMMENT '执行结束时间',
    `exec_status` TINYINT     NOT NULL DEFAULT 0 COMMENT '执行状态：0-执行中，1-成功，2-失败',
    `result_data` TEXT                 COMMENT '算法输出结果（JSON）',
    `error_msg`   TEXT                 COMMENT '错误信息',
    `duration_ms` BIGINT               COMMENT '执行耗时（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_start_time` (`start_time`),
    UNIQUE KEY `uk_rule_window` (`rule_id`, `window_key`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则执行日志表';

-- ------------------------------------------------------------
-- 6. 规则引擎系统配置表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rule_engine_config` (
    `id`           VARCHAR(32)  NOT NULL COMMENT '主键',
    `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500)          COMMENT '配置值',
    `config_desc`  VARCHAR(200)          COMMENT '配置说明',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则引擎系统配置';

INSERT IGNORE INTO rule_engine_config VALUES
    (REPLACE(UUID(),'-',''), 'flink.rest.url',        'http://localhost:8081',                    'Flink REST 地址'),
    (REPLACE(UUID(),'-',''), 'tdengine.jdbc.url',     'jdbc:TAOS-RS://localhost:6041/bit_iot_ts', 'TDEngine JDBC URL'),
    (REPLACE(UUID(),'-',''), 'tdengine.username',     'root',                                     'TDEngine 用户名'),
    (REPLACE(UUID(),'-',''), 'tdengine.password',     'taosdata',                                 'TDEngine 密码'),
    (REPLACE(UUID(),'-',''), 'tdengine.super.table',  'device_data',                              'TDEngine 设备数据超级表'),
    (REPLACE(UUID(),'-',''), 'mqtt.broker.url',       'tcp://localhost:1883',                     'MQTT Broker 地址');

-- ============================================================
-- 如果是从旧表升级，使用以下 ALTER 语句
-- ============================================================
-- ALTER TABLE rule_config
--     ADD COLUMN `flink_job_id`   VARCHAR(64)  COMMENT 'Flink Job ID' AFTER rule_status,
--     ADD COLUMN `key_strategy`   VARCHAR(20)  DEFAULT 'device_point' COMMENT '分组策略' AFTER window_unit,
--     ADD COLUMN `window_slide`   BIGINT       COMMENT '滑动窗口步长' AFTER window_size,
--     ADD COLUMN `parallelism`    INT          DEFAULT 2 COMMENT '并行度' AFTER key_strategy;
--
-- ALTER TABLE rule_execution_log
--     ADD COLUMN `window_key` VARCHAR(100) COMMENT '窗口键（deviceId 或 deviceId#pointCode）' AFTER `rule_id`,
--     ADD UNIQUE KEY `uk_rule_window` (`rule_id`, `window_key`, `start_time`, `end_time`);


-- ============================================================
-- TDEngine 结果超级表（在 TDEngine 中执行，非 MySQL）
-- ============================================================
-- CREATE STABLE IF NOT EXISTS rule_result_stable (
--     ts           TIMESTAMP,
--     rule_id      NCHAR(32),
--     data_key     NCHAR(100),
--     metric_name  NCHAR(100),
--     metric_value DOUBLE
-- ) TAGS (
--     tag_rule_id NCHAR(32)
-- );
