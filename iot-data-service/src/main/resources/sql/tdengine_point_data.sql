CREATE DATABASE IF NOT EXISTS iot_data;

DROP STABLE IF EXISTS iot_data.point_data;

CREATE STABLE IF NOT EXISTS iot_data.point_data (ts TIMESTAMP, `value` DOUBLE, quality TINYINT) TAGS (device_id NCHAR(64), point_code NCHAR(64));

CREATE TABLE IF NOT EXISTS iot_data.point_data_test_temp_001 USING iot_data.point_data TAGS ('test-device-001', 'TEMP_001');

CREATE TABLE IF NOT EXISTS iot_data.point_data_test_press_001 USING iot_data.point_data TAGS ('test-device-001', 'PRESS_001');

INSERT INTO iot_data.point_data_test_temp_001 VALUES (NOW, 25.5, 0);
INSERT INTO iot_data.point_data_test_press_001 VALUES (NOW, 0.82, 0);
