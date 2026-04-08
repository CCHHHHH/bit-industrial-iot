CREATE TABLE IF NOT EXISTS simulator_device (
    id VARCHAR(32) PRIMARY KEY,
    device_name VARCHAR(100) NOT NULL,
    device_code VARCHAR(100) NOT NULL,
    device_status VARCHAR(32) NOT NULL DEFAULT 'ONLINE',
    device_type VARCHAR(64) NOT NULL,
    device_location VARCHAR(255) NULL,
    device_ip VARCHAR(64) NULL,
    device_mac VARCHAR(64) NULL,
    firmware_version VARCHAR(64) NULL,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_simulator_device_code (device_code)
);

CREATE TABLE IF NOT EXISTS simulator_point (
    id VARCHAR(32) PRIMARY KEY,
    device_id VARCHAR(32) NOT NULL,
    point_code VARCHAR(100) NOT NULL,
    point_name VARCHAR(100) NOT NULL,
    unit VARCHAR(32) NULL,
    min_value DOUBLE NOT NULL,
    max_value DOUBLE NOT NULL,
    precision_scale INT NOT NULL DEFAULT 2,
    quality INT NOT NULL DEFAULT 0,
    register_address INT NULL,
    register_type VARCHAR(32) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_simulator_point_device_code (device_id, point_code),
    KEY idx_simulator_point_device_id (device_id)
);

CREATE TABLE IF NOT EXISTS simulator_task (
    id VARCHAR(32) PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    device_id VARCHAR(32) NOT NULL,
    protocol_type VARCHAR(32) NOT NULL,
    task_status VARCHAR(32) NOT NULL DEFAULT 'STOPPED',
    frequency_ms BIGINT NOT NULL DEFAULT 1000,
    http_url VARCHAR(500) NULL,
    http_method VARCHAR(16) NULL,
    mqtt_broker_url VARCHAR(255) NULL,
    mqtt_topic VARCHAR(255) NULL,
    mqtt_username VARCHAR(100) NULL,
    mqtt_password VARCHAR(100) NULL,
    kafka_bootstrap_servers VARCHAR(255) NULL,
    kafka_topic VARCHAR(255) NULL,
    modbus_host VARCHAR(255) NULL,
    modbus_port INT NULL,
    modbus_unit_id INT NULL,
    payload_template TEXT NULL,
    last_sent_time DATETIME NULL,
    last_error VARCHAR(500) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_simulator_task_device_id (device_id),
    KEY idx_simulator_task_status (task_status),
    KEY idx_simulator_task_protocol (protocol_type)
);

CREATE TABLE IF NOT EXISTS simulator_timeseries_data (
    id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) NOT NULL,
    device_id VARCHAR(32) NOT NULL,
    point_code VARCHAR(100) NOT NULL,
    point_name VARCHAR(100) NOT NULL,
    point_value DOUBLE NOT NULL,
    unit VARCHAR(32) NULL,
    quality INT NOT NULL DEFAULT 0,
    protocol_type VARCHAR(32) NOT NULL,
    generated_time DATETIME NOT NULL,
    KEY idx_simulator_data_device_time (device_id, generated_time),
    KEY idx_simulator_data_point_time (point_code, generated_time)
);

CREATE TABLE IF NOT EXISTS simulator_send_log (
    id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) NOT NULL,
    device_id VARCHAR(32) NOT NULL,
    protocol_type VARCHAR(32) NOT NULL,
    payload_json LONGTEXT NULL,
    send_status VARCHAR(32) NOT NULL,
    error_message VARCHAR(500) NULL,
    sent_time DATETIME NOT NULL,
    KEY idx_simulator_send_log_task (task_id, sent_time),
    KEY idx_simulator_send_log_status (send_status, sent_time)
);

INSERT INTO simulator_device (
    id, device_name, device_code, device_status, device_type, device_location, device_ip, device_mac, firmware_version, remark, create_time, update_time
) VALUES (
    'sim-device-001', '模拟温度传感器-001', 'SIM-TEMP-001', 'ONLINE', 'temperature', 'A区产线', '192.168.10.101', '00:16:3E:AA:BB:01', 'v1.0.0', '默认演示设备', NOW(), NOW()
) ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO simulator_point (
    id, device_id, point_code, point_name, unit, min_value, max_value, precision_scale, quality, register_address, register_type, create_time, update_time
) VALUES
('sim-point-001', 'sim-device-001', 'temp', '温度', 'C', 18, 42, 2, 0, 40001, 'holding', NOW(), NOW()),
('sim-point-002', 'sim-device-001', 'humidity', '湿度', '%', 35, 88, 2, 0, 40002, 'holding', NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO simulator_task (
    id, task_name, device_id, protocol_type, task_status, frequency_ms, http_url, http_method,
    mqtt_broker_url, mqtt_topic, kafka_bootstrap_servers, kafka_topic, modbus_host, modbus_port, modbus_unit_id,
    payload_template, last_sent_time, last_error, create_time, update_time
) VALUES
('sim-task-http-001', 'HTTP模拟发送任务', 'sim-device-001', 'HTTP', 'STOPPED', 3000, 'http://127.0.0.1:8088/mock/iot', 'POST', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
('sim-task-mqtt-001', 'MQTT模拟发送任务', 'sim-device-001', 'MQTT', 'STOPPED', 3000, NULL, NULL, 'tcp://127.0.0.1:1883', 'simulator/device/SIM-TEMP-001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
('sim-task-kafka-001', 'Kafka模拟发送任务', 'sim-device-001', 'KAFKA', 'STOPPED', 3000, NULL, NULL, NULL, NULL, '127.0.0.1:9092', 'simulator-device-data', NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
('sim-task-modbus-001', 'Modbus寄存器模拟任务', 'sim-device-001', 'MODBUS_TCP', 'STOPPED', 3000, NULL, NULL, NULL, NULL, NULL, NULL, '127.0.0.1', 502, 1, NULL, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();
