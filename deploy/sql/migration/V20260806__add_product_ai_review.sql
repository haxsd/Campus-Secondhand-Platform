CREATE TABLE IF NOT EXISTS `ai_agent_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_id` VARCHAR(64) NOT NULL,
  `agent_type` VARCHAR(40) NOT NULL,
  `product_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `submitted_product_version` INT NOT NULL,
  `rule_version` VARCHAR(40) NOT NULL,
  `model_name` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(30) NOT NULL COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/TIMEOUT/INVALID_OUTPUT/STALE/DISABLED',
  `attempt` INT NOT NULL DEFAULT 0 COMMENT '执行尝试次数，至少为1',
  `decision` VARCHAR(30) DEFAULT NULL,
  `risk_level` VARCHAR(20) DEFAULT NULL,
  `confidence` DECIMAL(5,4) DEFAULT NULL,
  `input_snapshot` JSON NOT NULL,
  `result_json` JSON DEFAULT NULL,
  `error_code` VARCHAR(50) DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `finished_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_run_id` (`run_id`),
  KEY `idx_product_created` (`product_id`, `created_at`, `id`),
  KEY `idx_status_updated` (`status`, `updated_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品合规AI审核运行记录';

CREATE TABLE IF NOT EXISTS `ai_rule_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_version` VARCHAR(40) NOT NULL,
  `domain` VARCHAR(40) NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `effective_at` DATETIME NOT NULL,
  `expired_at` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_version_domain` (`domain`, `rule_version`),
  KEY `idx_rule_effective` (`domain`, `status`, `effective_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI商品审核规则版本';

CREATE TABLE IF NOT EXISTS `ai_rule_fragment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_set_id` BIGINT NOT NULL,
  `rule_id` VARCHAR(60) NOT NULL,
  `rule_version` VARCHAR(40) NOT NULL,
  `domain` VARCHAR(40) NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content_hash` VARCHAR(128) NOT NULL,
  `vector_doc_id` VARCHAR(128) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_fragment` (`domain`, `rule_id`, `rule_version`),
  KEY `idx_rule_set` (`rule_set_id`, `deleted`),
  KEY `idx_rule_filter` (`domain`, `rule_version`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI商品审核规则片段';

CREATE TABLE IF NOT EXISTS `ai_rule_reference` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_id` VARCHAR(64) NOT NULL,
  `rule_set_id` BIGINT NOT NULL,
  `rule_id` VARCHAR(60) NOT NULL,
  `rule_version` VARCHAR(40) NOT NULL,
  `domain` VARCHAR(40) NOT NULL,
  `chunk_id` VARCHAR(128) DEFAULT NULL,
  `retrieval_score` DECIMAL(10,6) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_run_rule_chunk` (`run_id`, `rule_id`, `rule_version`, `chunk_id`),
  KEY `idx_run` (`run_id`, `id`),
  KEY `idx_rule` (`domain`, `rule_version`, `rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI审核命中的规则引用';

ALTER TABLE `product_review_log`
  MODIFY COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID，AI审核时为空',
  MODIFY COLUMN `result` TINYINT NOT NULL COMMENT '1通过 2驳回 3建议人工复核';

SET @has_operator_type := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'product_review_log' AND column_name = 'operator_type'
);
SET @sql := IF(@has_operator_type = 0,
  'ALTER TABLE product_review_log ADD COLUMN operator_type TINYINT NOT NULL DEFAULT 0 COMMENT ''0人工 1 AI''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_run_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'product_review_log' AND column_name = 'run_id'
);
SET @sql := IF(@has_run_id = 0,
  'ALTER TABLE product_review_log ADD COLUMN run_id VARCHAR(64) DEFAULT NULL COMMENT ''AI运行ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_run_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'product_review_log' AND index_name = 'idx_run'
);
SET @sql := IF(@has_run_index = 0,
  'ALTER TABLE product_review_log ADD KEY idx_run (run_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
