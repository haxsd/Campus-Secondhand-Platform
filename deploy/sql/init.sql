-- =====================================================================
-- 校园二手交易平台 · 建表脚本（第一版）
-- MySQL 8.x · utf8mb4 · InnoDB
-- 设计口径：见《简历项目文档》1.10
--   - 状态字段存 TINYINT 稳定编码，含义写在注释里，由后端枚举维护
--   - 不声明物理外键，关联完整性由应用层 + 唯一约束 + 状态机保证
--   - 金额一律 DECIMAL(10,2)；ID 用 BIGINT 自增，返回前端转字符串
--   - 历史订单/快照/评价/纠纷不物理删除
-- =====================================================================

CREATE DATABASE IF NOT EXISTS campus_trade
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE campus_trade;

-- ---------------------------------------------------------------------
-- 1. 用户与信用
-- ---------------------------------------------------------------------

-- 用户表
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `student_no`  VARCHAR(32)  NOT NULL COMMENT '学号（登录账号之一）',
  `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号（登录账号之一）',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `nickname`    VARCHAR(30)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `campus`      VARCHAR(30)  DEFAULT NULL COMMENT '所在校区',
  `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色 0普通用户 1管理员',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0正常 1封禁',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB COMMENT='用户表';

-- 用户信用摘要表（读模型，注册时初始化，评价后异步重算）
CREATE TABLE `user_credit_summary` (
  `user_id`          BIGINT        NOT NULL COMMENT '用户ID',
  `credit_score`     INT           NOT NULL DEFAULT 100 COMMENT '信用分 60~100',
  `deal_count`       INT           NOT NULL DEFAULT 0 COMMENT '成交次数（订单完成时+1）',
  `review_count`     INT           NOT NULL DEFAULT 0 COMMENT '收到的可见评价数',
  `avg_rating`       DECIMAL(3,2)  NOT NULL DEFAULT 0 COMMENT '平均评分',
  `good_review_rate` DECIMAL(5,4)  NOT NULL DEFAULT 0 COMMENT '好评率（>=4分占比）',
  `bad_review_count` INT           NOT NULL DEFAULT 0 COMMENT '差评数（<=2分）',
  `version`          INT           NOT NULL DEFAULT 0 COMMENT '重算版本号，防旧任务覆盖',
  `calculated_at`    DATETIME      DEFAULT NULL COMMENT '最近重算时间',
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB COMMENT='用户信用摘要表';

-- ---------------------------------------------------------------------
-- 2. 商品域
-- ---------------------------------------------------------------------

-- 商品分类表
CREATE TABLE `category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(30) NOT NULL COMMENT '分类名',
  `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序值，小的在前',
  `enabled`    TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB COMMENT='商品分类表';

-- 商品主表
-- status: 0草稿 1待人工审核 2审核驳回 3在售 4已下架 5已售罄
CREATE TABLE `product` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `seller_id`   BIGINT        NOT NULL COMMENT '卖家ID（只取 UserContext）',
  `category_id` BIGINT        NOT NULL COMMENT '分类ID',
  `title`       VARCHAR(60)   NOT NULL COMMENT '标题',
  `description` VARCHAR(2000) NOT NULL COMMENT '描述',
  `price`       DECIMAL(10,2) NOT NULL COMMENT '单价',
  `stock`       INT           NOT NULL DEFAULT 1 COMMENT '当前可售库存',
  `item_condition` TINYINT   NOT NULL DEFAULT 0 COMMENT '成色 0全新 1几乎全新 2轻微使用 3明显使用（condition 是 MySQL 保留字，故加前缀）',
  `campus`      VARCHAR(30)   NOT NULL COMMENT '所在校区',
  `trade_place` VARCHAR(60)   DEFAULT NULL COMMENT '默认交易地点',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '0草稿 1待审核 2驳回 3在售 4已下架 5已售罄',
  `view_count`  INT           NOT NULL DEFAULT 0 COMMENT '浏览次数（异步累计）',
  `version`     INT           NOT NULL DEFAULT 0 COMMENT '编辑乐观锁版本号',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除 0否 1是',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_list` (`status`, `campus`, `category_id`, `price`, `id`),
  KEY `idx_seller` (`seller_id`, `status`)
) ENGINE=InnoDB COMMENT='商品主表';

-- 商品图片表
CREATE TABLE `product_image` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT       NOT NULL,
  `url`        VARCHAR(255) NOT NULL COMMENT '图片地址（受控对象地址）',
  `sort`       INT          NOT NULL DEFAULT 0,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_sort` (`product_id`, `sort`)
) ENGINE=InnoDB COMMENT='商品图片表';

-- 商品审核日志表
CREATE TABLE `product_review_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `product_id`  BIGINT       NOT NULL,
  `reviewer_id` BIGINT       NOT NULL COMMENT '审核人（管理员）',
  `result`      TINYINT      NOT NULL COMMENT '1通过 2驳回',
  `reason`      VARCHAR(500) DEFAULT NULL COMMENT '驳回原因/修改建议',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商品人工审核日志';

-- 浏览记录表
CREATE TABLE `browse_history` (
  `id`              BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT   NOT NULL,
  `product_id`      BIGINT   NOT NULL,
  `last_browsed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后浏览时间（重复浏览只更新此列）',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_time` (`user_id`, `last_browsed_at`)
) ENGINE=InnoDB COMMENT='浏览记录表（upsert）';

-- ---------------------------------------------------------------------
-- 3. 交易域
-- ---------------------------------------------------------------------

-- 交易订单表
-- status: 0待卖家确认 1已确认 2已完成 3已取消 4超时取消 5纠纷中
CREATE TABLE `trade_order` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`         VARCHAR(32)   NOT NULL COMMENT '业务订单号（后端生成，前端按字符串处理）',
  `request_id`       VARCHAR(64)   NOT NULL COMMENT '客户端幂等ID',
  `buyer_id`         BIGINT        NOT NULL,
  `seller_id`        BIGINT        NOT NULL,
  `product_id`       BIGINT        NOT NULL,
  `quantity`         INT           NOT NULL COMMENT '购买数量',
  `unit_price`       DECIMAL(10,2) NOT NULL COMMENT '下单时单价（后端按库中价格计算）',
  `total_amount`     DECIMAL(10,2) NOT NULL COMMENT 'unit_price * quantity',
  `trade_time`       DATETIME      DEFAULT NULL COMMENT '约定交易时间',
  `trade_place`      VARCHAR(60)   DEFAULT NULL COMMENT '约定交易地点',
  `remark`           VARCHAR(200)  DEFAULT NULL,
  `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已完成 3已取消 4超时取消 5纠纷中',
  `status_before_dispute` TINYINT  DEFAULT NULL COMMENT '进入纠纷前的状态，用于驳回纠纷时恢复',
  `confirm_deadline` DATETIME      NOT NULL COMMENT '卖家确认截止时间',
  `finished_at`      DATETIME      DEFAULT NULL COMMENT '完成时间',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_buyer_request` (`buyer_id`, `request_id`),
  KEY `idx_buyer` (`buyer_id`, `status`),
  KEY `idx_seller` (`seller_id`, `status`),
  KEY `idx_timeout_scan` (`status`, `confirm_deadline`, `id`)
) ENGINE=InnoDB COMMENT='交易订单表';

-- 商品快照表（与订单一对一，保存下单那一刻买家看到的信息）
CREATE TABLE `product_snapshot` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `order_id`    BIGINT        NOT NULL,
  `product_id`  BIGINT        NOT NULL,
  `title`       VARCHAR(60)   NOT NULL,
  `description` VARCHAR(2000) NOT NULL,
  `price`       DECIMAL(10,2) NOT NULL,
  `item_condition` TINYINT   NOT NULL,
  `campus`      VARCHAR(30)   NOT NULL,
  `trade_place` VARCHAR(60)   DEFAULT NULL,
  `images`      JSON          DEFAULT NULL COMMENT '图片地址列表',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`)
) ENGINE=InnoDB COMMENT='下单商品快照';

-- 订单状态日志表
CREATE TABLE `trade_order_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `order_id`      BIGINT       NOT NULL,
  `from_status`   TINYINT      NOT NULL,
  `to_status`     TINYINT      NOT NULL,
  `operator_type` TINYINT      NOT NULL COMMENT '0买家 1卖家 2系统 3管理员',
  `operator_id`   BIGINT       DEFAULT NULL COMMENT '系统操作时为空',
  `reason`        VARCHAR(200) DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB COMMENT='订单状态流转日志';

-- ---------------------------------------------------------------------
-- 4. 交易后域
-- ---------------------------------------------------------------------

-- 交易评价表（第一版：一单一评，买家评卖家）
CREATE TABLE `trade_review` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `order_id`    BIGINT       NOT NULL,
  `reviewer_id` BIGINT       NOT NULL COMMENT '评价人（买家）',
  `seller_id`   BIGINT       NOT NULL COMMENT '被评价卖家（由订单推导）',
  `rating`      TINYINT      NOT NULL COMMENT '1~5 分',
  `content`     VARCHAR(500) DEFAULT NULL,
  `tags`        VARCHAR(200) DEFAULT NULL COMMENT '标签，逗号分隔',
  `visible`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1可见 0被管理员隐藏',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`),
  KEY `idx_seller` (`seller_id`, `visible`, `created_at`)
) ENGINE=InnoDB COMMENT='交易评价表';

-- 纠纷表（第一版：一单一纠纷）
-- status: 0待处理 1待补充材料 2已驳回 3维持完成 4取消交易
CREATE TABLE `dispute` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `order_id`      BIGINT        NOT NULL,
  `applicant_id`  BIGINT        NOT NULL COMMENT '申请人（订单买家或卖家）',
  `respondent_id` BIGINT        NOT NULL COMMENT '被投诉方（由订单关系推导）',
  `reason_type`   TINYINT       NOT NULL COMMENT '纠纷原因分类 0货不对板 1未履约 2其他',
  `statement`     VARCHAR(2000) NOT NULL COMMENT '申请人文字说明（当事人主张）',
  `evidence`      JSON          DEFAULT NULL COMMENT '证据材料地址列表',
  `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '0待处理 1待补材料 2驳回 3维持完成 4取消交易',
  `handler_id`    BIGINT        DEFAULT NULL COMMENT '处理管理员',
  `handle_note`   VARCHAR(1000) DEFAULT NULL COMMENT '处理说明',
  `handled_at`    DATETIME      DEFAULT NULL,
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`),
  KEY `idx_status` (`status`, `created_at`, `id`)
) ENGINE=InnoDB COMMENT='纠纷表';

-- ---------------------------------------------------------------------
-- 5. 初始数据
-- ---------------------------------------------------------------------

INSERT INTO `category` (`name`, `sort`) VALUES
('数码电子', 1), ('图书教材', 2), ('生活用品', 3),
('运动户外', 4), ('服饰美妆', 5), ('其他', 99);

-- 管理员账号：学号 admin001 / 手机 13800000000 / 密码 Admin@123（BCrypt 需在应用启动时或手动生成后替换）
-- INSERT INTO `user` (student_no, phone, password, nickname, role) VALUES ('admin001','13800000000','<bcrypt>','管理员',1);
-- INSERT INTO `user_credit_summary` (user_id) VALUES (1);
