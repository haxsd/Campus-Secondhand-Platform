-- 通过 information_schema 判断表是否存在，避免重复执行时依赖 MySQL 版本特性。
SET @table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'dispute_agent_run'
);
SET @create_sql := IF(
    @table_exists = 0,
    'CREATE TABLE dispute_agent_run (id BIGINT NOT NULL AUTO_INCREMENT, run_id VARCHAR(64) NOT NULL, dispute_id BIGINT NOT NULL, order_id BIGINT NOT NULL, status VARCHAR(24) NOT NULL, attempt INT NOT NULL DEFAULT 0, model_name VARCHAR(100) NOT NULL, rule_version VARCHAR(40) NOT NULL, submitted_evidence_version INT NOT NULL, input_snapshot JSON NOT NULL, input_digest CHAR(64) NOT NULL, result_json JSON NULL, error_code VARCHAR(64) NULL, error_message VARCHAR(500) NULL, triggered_by BIGINT NOT NULL, adopted_by BIGINT NULL, adopted_at DATETIME NULL, adopted_action VARCHAR(32) NULL, started_at DATETIME NULL, finished_at DATETIME NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY(id), UNIQUE KEY uk_dispute_agent_run_id(run_id), KEY idx_dispute_agent_dispute_created(dispute_id,created_at,id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT="纠纷辅助 Agent 运行记录"',
    'SELECT 1'
);
PREPARE create_statement FROM @create_sql;
EXECUTE create_statement;
DEALLOCATE PREPARE create_statement;
