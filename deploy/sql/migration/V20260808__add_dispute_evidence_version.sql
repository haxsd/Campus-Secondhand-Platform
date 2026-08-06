-- 纠纷证据版本和追加流水迁移。使用信息_schema 查询，兼容当前 MySQL 版本并支持重复执行。
SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dispute'
      AND column_name = 'evidence_version'
);

SET @alter_sql := IF(
    @column_exists = 0,
    'ALTER TABLE `dispute` ADD COLUMN `evidence_version` INT NOT NULL DEFAULT 1 COMMENT ''当前证据版本''',
    'SELECT 1'
);

PREPARE alter_dispute FROM @alter_sql;
EXECUTE alter_dispute;
DEALLOCATE PREPARE alter_dispute;

CREATE TABLE IF NOT EXISTS `dispute_evidence_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dispute_id` BIGINT NOT NULL COMMENT '关联纠纷',
  `operator_id` BIGINT NOT NULL COMMENT '追加材料的用户',
  `operator_role` TINYINT NOT NULL COMMENT '0申请人 1被申请人',
  `evidence_version` INT NOT NULL COMMENT '本次追加后的版本',
  `statement` VARCHAR(2000) DEFAULT NULL COMMENT '本次补充说明',
  `evidence` JSON DEFAULT NULL COMMENT '本次追加的证据地址列表',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dispute_created` (`dispute_id`, `created_at`, `id`),
  KEY `idx_dispute_version` (`dispute_id`, `evidence_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纠纷证据追加流水';
