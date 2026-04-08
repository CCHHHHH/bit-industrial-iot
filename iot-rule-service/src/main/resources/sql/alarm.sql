-- ============================================================
-- 告警模块建表 SQL
-- 数据库：bit_iot (MySQL)
-- ============================================================

USE bit_iot;

CREATE TABLE IF NOT EXISTS `alarm_record` (
    `id`                 VARCHAR(32)  NOT NULL COMMENT '主键',
    `source_type`        VARCHAR(20)  NOT NULL DEFAULT 'rule' COMMENT '来源类型：rule',
    `source_id`          VARCHAR(32)           DEFAULT NULL COMMENT '来源业务主键',
    `rule_id`            VARCHAR(32)           DEFAULT NULL COMMENT '规则 ID',
    `rule_name`          VARCHAR(100)          DEFAULT NULL COMMENT '规则名称',
    `device_id`          VARCHAR(32)           DEFAULT NULL COMMENT '设备 ID',
    `device_name`        VARCHAR(100)          DEFAULT NULL COMMENT '设备名称',
    `point_code`         VARCHAR(100)          DEFAULT NULL COMMENT '测点编码',
    `dedup_key`          VARCHAR(200) NOT NULL COMMENT '活跃告警去重键',
    `alarm_title`        VARCHAR(200) NOT NULL COMMENT '告警标题',
    `alarm_message`      VARCHAR(500) NOT NULL COMMENT '告警内容',
    `alarm_level`        VARCHAR(20)  NOT NULL DEFAULT 'warning' COMMENT '告警级别：info/warning/error',
    `alarm_status`       VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '告警状态：active/resolved',
    `trigger_count`      INT          NOT NULL DEFAULT 1 COMMENT '触发次数',
    `first_trigger_time` DATETIME     NOT NULL COMMENT '首次触发时间',
    `last_trigger_time`  DATETIME     NOT NULL COMMENT '最近触发时间',
    `resolved_time`      DATETIME              DEFAULT NULL COMMENT '解决时间',
    `metric_name`        VARCHAR(100)          DEFAULT NULL COMMENT '指标名称',
    `metric_value`       VARCHAR(100)          DEFAULT NULL COMMENT '指标值',
    `result_data`        TEXT                  COMMENT '算法结果 JSON',
    `create_time`        DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`        DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_active_dedup` (`dedup_key`, `alarm_status`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_alarm_status` (`alarm_status`),
    KEY `idx_alarm_level` (`alarm_level`),
    KEY `idx_last_trigger_time` (`last_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

INSERT INTO `alarm_record`
(`id`, `source_type`, `source_id`, `rule_id`, `rule_name`, `device_id`, `device_name`, `point_code`,
 `dedup_key`, `alarm_title`, `alarm_message`, `alarm_level`, `alarm_status`, `trigger_count`,
 `first_trigger_time`, `last_trigger_time`, `resolved_time`, `metric_name`, `metric_value`,
 `result_data`, `create_time`, `update_time`)
VALUES
('alarm001', 'rule', 'rule001', 'rule001', '温度阈值规则', 'device001', '温湿度传感器-001', 'temperature',
 'rule:rule001:device001:temperature', '温度超出阈值', '温度超出阈值', 'error', 'active', 3,
 '2026-02-04 10:10:00', '2026-02-04 10:30:00', NULL, 'temperature', '89.6',
 '{"alert":true,"alertLevel":"error","alertMessage":"温度超出阈值","metricName":"temperature","metricValue":"89.6"}',
 '2026-02-04 10:10:00', '2026-02-04 10:30:00'),
('alarm002', 'rule', 'rule002', 'rule002', '气压异常规则', 'device002', '气压传感器-001', 'pressure',
 'rule:rule002:device002:pressure', '气压异常', '气压异常', 'warning', 'active', 2,
 '2026-02-04 09:00:00', '2026-02-04 09:15:00', NULL, 'pressure', '0.72',
 '{"alert":true,"alertLevel":"warning","alertMessage":"气压异常","metricName":"pressure","metricValue":"0.72"}',
 '2026-02-04 09:00:00', '2026-02-04 09:15:00'),
('alarm003', 'rule', 'rule003', 'rule003', '光照不足规则', 'device003', '光照传感器-001', 'illumination',
 'rule:rule003:device003:illumination', '光照强度不足', '光照强度不足', 'info', 'active', 1,
 '2026-02-04 08:45:00', '2026-02-04 08:45:00', NULL, 'illumination', '115',
 '{"alert":true,"alertLevel":"info","alertMessage":"光照强度不足","metricName":"illumination","metricValue":"115"}',
 '2026-02-04 08:45:00', '2026-02-04 08:45:00'),
('alarm004', 'rule', 'rule004', 'rule004', '烟雾浓度规则', 'device004', '烟雾传感器-001', 'smoke',
 'rule:rule004:device004:smoke', '烟雾浓度过高', '烟雾浓度过高', 'error', 'resolved', 1,
 '2026-02-04 07:20:00', '2026-02-04 07:20:00', '2026-02-04 07:35:00', 'smoke', '12.8',
 '{"alert":true,"alertLevel":"error","alertMessage":"烟雾浓度过高","metricName":"smoke","metricValue":"12.8"}',
 '2026-02-04 07:20:00', '2026-02-04 07:35:00'),
('alarm005', 'rule', 'rule005', 'rule005', '湿度异常规则', 'device005', '温湿度传感器-002', 'humidity',
 'rule:rule005:device005:humidity', '湿度异常', '湿度异常', 'warning', 'resolved', 1,
 '2026-02-04 06:50:00', '2026-02-04 06:50:00', '2026-02-04 07:05:00', 'humidity', '92.1',
 '{"alert":true,"alertLevel":"warning","alertMessage":"湿度异常","metricName":"humidity","metricValue":"92.1"}',
 '2026-02-04 06:50:00', '2026-02-04 07:05:00')
ON DUPLICATE KEY UPDATE
    `alarm_title` = VALUES(`alarm_title`),
    `alarm_message` = VALUES(`alarm_message`),
    `alarm_level` = VALUES(`alarm_level`),
    `alarm_status` = VALUES(`alarm_status`),
    `trigger_count` = VALUES(`trigger_count`),
    `last_trigger_time` = VALUES(`last_trigger_time`),
    `resolved_time` = VALUES(`resolved_time`),
    `metric_name` = VALUES(`metric_name`),
    `metric_value` = VALUES(`metric_value`),
    `result_data` = VALUES(`result_data`),
    `update_time` = VALUES(`update_time`);
