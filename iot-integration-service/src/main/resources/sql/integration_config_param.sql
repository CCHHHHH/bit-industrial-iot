SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'integration_config_param'
              AND COLUMN_NAME = 'param_desc'
        ),
        'SELECT 1',
        'ALTER TABLE integration_config_param ADD COLUMN param_desc VARCHAR(255) NULL COMMENT ''插件配置参数描述'' AFTER param_value'
    )
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
