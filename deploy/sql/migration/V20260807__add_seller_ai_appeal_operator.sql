-- 可重复执行：补充审核日志 operator_type 的取值语义。
-- 0=管理员人工审核，1=AI审核，2=卖家申请人工复核。
ALTER TABLE `product_review_log`
  MODIFY COLUMN `operator_type` TINYINT NOT NULL DEFAULT 0;
