-- 纠纷管理列表性能升级（MySQL 8）
--
-- 执行前先在目标库确认已有索引，避免重复创建：
-- SHOW INDEX FROM dispute;
--
-- 本项目没有引入 Flyway/Liquibase，因此本文件作为一次性人工迁移脚本执行；
-- 执行完成后应使用 EXPLAIN ANALYZE 验证列表 SQL 是否命中预期索引。

-- 一个订单只能发起一条纠纷：既是业务约束，也让 existsByOrderId 查询走唯一索引。
-- 若线上数据库已存在同名唯一约束，请不要重复执行本语句。
CREATE UNIQUE INDEX uk_dispute_order_id ON dispute (order_id);

-- 管理端按指定状态筛选，并按创建时间倒序分页时使用。
-- id 是同一秒内 created_at 相同记录的稳定排序补充，避免翻页时顺序不确定。
CREATE INDEX idx_dispute_status_created_id
    ON dispute (status, created_at DESC, id DESC);

-- 管理端查询全部状态时无法使用以上索引完成全局时间排序，因此保留这一条索引。
CREATE INDEX idx_dispute_created_id
    ON dispute (created_at DESC, id DESC);

-- 游标分页示例：cursorCreatedAt 与 cursorId 来自上一页最后一条展示数据。
-- 这条查询会从复合索引的指定位置继续向后扫描，不需要 OFFSET 跳过深分页前的记录。
-- EXPLAIN ANALYZE
-- SELECT d.id
-- FROM dispute d
-- WHERE d.status = 0
--   AND (d.created_at < '2026-07-27 12:00:00'
--        OR (d.created_at = '2026-07-27 12:00:00' AND d.id < 702))
-- ORDER BY d.created_at DESC, d.id DESC
-- LIMIT 21; -- pageSize 为 20 时多取一条，用于计算 hasNext
