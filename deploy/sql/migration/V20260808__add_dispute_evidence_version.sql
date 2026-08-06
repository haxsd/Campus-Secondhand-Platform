-- Idempotent dispute evidence version and append log migration.
SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'dispute'
    AND column_name = 'evidence_version'
);
SET @alter_sql := IF(
  @column_exists = 0,
  'ALTER TABLE `dispute` ADD COLUMN `evidence_version` INT NOT NULL DEFAULT 1 COMMENT ''current evidence version''',
  'SELECT 1'
);
PREPARE alter_dispute FROM @alter_sql;
EXECUTE alter_dispute;
DEALLOCATE PREPARE alter_dispute;

CREATE TABLE IF NOT EXISTS `dispute_evidence_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dispute_id` BIGINT NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `operator_role` TINYINT NOT NULL COMMENT '0 applicant 1 respondent',
  `evidence_version` INT NOT NULL COMMENT 'version after append',
  `statement` VARCHAR(2000) DEFAULT NULL COMMENT 'supplement statement',
  `evidence` JSON DEFAULT NULL COMMENT 'new evidence URLs',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dispute_created` (`dispute_id`, `created_at`, `id`),
  KEY `idx_dispute_version` (`dispute_id`, `evidence_version`)
) ENGINE=InnoDB COMMENT='dispute evidence append log';
