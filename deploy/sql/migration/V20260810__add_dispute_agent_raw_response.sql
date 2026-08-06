-- 保存截断后的模型原文，便于定位结构化输出失败原因。
SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dispute_agent_run'
      AND column_name = 'raw_response'
);
SET @alter_sql := IF(
    @column_exists = 0,
    'ALTER TABLE dispute_agent_run ADD COLUMN raw_response TEXT NULL COMMENT \"模型原始响应（最多保存 4000 字符）\" AFTER error_message',
    'SELECT 1'
);
PREPARE alter_statement FROM @alter_sql;
EXECUTE alter_statement;
DEALLOCATE PREPARE alter_statement;
