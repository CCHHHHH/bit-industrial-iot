CREATE TABLE alarm_record (
    id VARCHAR(32) PRIMARY KEY,
    source_type VARCHAR(20) NOT NULL DEFAULT 'rule',
    source_id VARCHAR(32),
    rule_id VARCHAR(32),
    rule_name VARCHAR(100),
    device_id VARCHAR(32),
    device_name VARCHAR(100),
    point_code VARCHAR(100),
    dedup_key VARCHAR(200) NOT NULL,
    alarm_title VARCHAR(200) NOT NULL,
    alarm_message VARCHAR(500) NOT NULL,
    alarm_level VARCHAR(20) NOT NULL DEFAULT 'warning',
    alarm_status VARCHAR(20) NOT NULL DEFAULT 'active',
    trigger_count INT NOT NULL DEFAULT 1,
    first_trigger_time TIMESTAMP NOT NULL,
    last_trigger_time TIMESTAMP NOT NULL,
    resolved_time TIMESTAMP,
    metric_name VARCHAR(100),
    metric_value VARCHAR(100),
    result_data CLOB,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_active_dedup ON alarm_record(dedup_key, alarm_status);
CREATE INDEX idx_rule_id ON alarm_record(rule_id);
CREATE INDEX idx_device_id ON alarm_record(device_id);
CREATE INDEX idx_alarm_status ON alarm_record(alarm_status);
CREATE INDEX idx_alarm_level ON alarm_record(alarm_level);
CREATE INDEX idx_last_trigger_time ON alarm_record(last_trigger_time);
