-- =====================================================================
-- 校园二手交易平台 · 初始业务数据
-- 适用环境：MySQL 8.x，数据库 campus_trade
--
-- 使用说明：
--   1. 请先执行 deploy/sql/init.sql 创建表结构；
--   2. 再执行本文件；
--   3. 本文件维护项目初始用户、分类、商品及必要的关联数据，不会清空业务表；
--   4. 可以重复执行：脚本只补充缺失记录，不会重置已有用户和商品的业务状态。
--
-- 初始登录账号：
--   运营账号：admin001 / Campus@2026
--   用户账号：20230001 ~ 20230012 / Campus@2026
--
-- 注意：
--   - 初始密码只用于首次登录，正式部署后应要求用户修改密码；
--   - 商品图片使用按商品关键词固定的网络图片，查看图片时需要网络；
--   - 这些数据按真实校园场景编写，可长期用于开发、联调和项目展示。
-- =====================================================================

USE campus_trade;

START TRANSACTION;

-- =====================================================================
-- 1. 分类数据
-- =====================================================================
-- category.name 有唯一索引，因此使用分类名作为稳定标识。
-- 重复执行时会恢复排序和启用状态，保证分类数据始终符合项目契约。
INSERT INTO category (`name`, `sort`, `enabled`)
VALUES
    ('数码电子', 1, 1),
    ('图书教材', 2, 1),
    ('生活用品', 3, 1),
    ('运动户外', 4, 1),
    ('服饰美妆', 5, 1),
    ('其他', 99, 1)
ON DUPLICATE KEY UPDATE
    `sort` = VALUES(`sort`),
    `enabled` = VALUES(`enabled`);

-- =====================================================================
-- 2. 用户数据
-- =====================================================================
-- 初始密码：Campus@2026
-- 哈希由 Spring Security BCryptPasswordEncoder(strength=10) 生成。
-- 即使数据库泄漏，也不会直接暴露初始密码明文。
SET @seed_password_hash =
    '$2a$10$eFpFtYWCjlFPUzsVO4RS2uOP0DxMggBO0LqXpzcLAYB07QxXiG4se';

-- 学号与手机号都有唯一索引。
-- 重复执行时不覆盖已存在的账号，避免重置后来修改过的密码、头像或账号状态。
INSERT INTO `user` (
    `student_no`,
    `phone`,
    `password`,
    `nickname`,
    `avatar`,
    `campus`,
    `role`,
    `status`
)
VALUES
    ('admin001', '13800000000', @seed_password_hash, '校园集市运营', NULL, '东校区', 1, 0),
    ('20230001', '13800000001', @seed_password_hash, '明明不熬夜', NULL, '东校区', 0, 0),
    ('20230002', '13800000002', @seed_password_hash, '数码小王', NULL, '东校区', 0, 0),
    ('20230003', '13800000003', @seed_password_hash, '阿May的书架', NULL, '西校区', 0, 0),
    ('20230004', '13800000004', @seed_password_hash, '毕业甩卖菌', NULL, '北校区', 0, 0),
    ('20230005', '13800000005', @seed_password_hash, '运动达人阿杰', NULL, '南校区', 0, 0),
    ('20230006', '13800000006', @seed_password_hash, '晴天收纳铺', NULL, '东校区', 0, 0),
    ('20230007', '13800000007', @seed_password_hash, '校园淘货客', NULL, '西校区', 0, 0),
    ('20230008', '13800000008', @seed_password_hash, '溪子的衣橱', NULL, '南校区', 0, 0),
    ('20230009', '13800000009', @seed_password_hash, '北区骑行者', NULL, '北校区', 0, 0),
    ('20230010', '13800000010', @seed_password_hash, '南区生活家', NULL, '南校区', 0, 0),
    ('20230011', '13800000011', @seed_password_hash, '代码与咖啡', NULL, '东校区', 0, 0),
    ('20230012', '13800000012', @seed_password_hash, '安然的小铺', NULL, '西校区', 0, 0)
ON DUPLICATE KEY UPDATE
    `student_no` = `student_no`;

-- 注册业务要求每个用户都拥有一条信用摘要。
-- 这里给几个主要卖家设置不同的信用数据，方便商品详情页观察展示效果。
INSERT INTO user_credit_summary (
    `user_id`,
    `credit_score`,
    `deal_count`,
    `review_count`,
    `avg_rating`,
    `good_review_rate`,
    `bad_review_count`,
    `version`,
    `calculated_at`
)
SELECT
    u.id,
    CASE u.student_no
        WHEN '20230002' THEN 98
        WHEN '20230003' THEN 95
        WHEN '20230004' THEN 90
        WHEN '20230005' THEN 96
        WHEN '20230008' THEN 97
        WHEN '20230009' THEN 94
        WHEN '20230010' THEN 99
        WHEN '20230011' THEN 97
        WHEN '20230012' THEN 93
        ELSE 100
    END AS credit_score,
    CASE u.student_no
        WHEN '20230001' THEN 5
        WHEN '20230002' THEN 23
        WHEN '20230003' THEN 12
        WHEN '20230004' THEN 8
        WHEN '20230005' THEN 15
        WHEN '20230008' THEN 18
        WHEN '20230009' THEN 10
        WHEN '20230010' THEN 7
        WHEN '20230011' THEN 14
        WHEN '20230012' THEN 9
        ELSE 0
    END AS deal_count,
    CASE u.student_no
        WHEN '20230001' THEN 5
        WHEN '20230002' THEN 21
        WHEN '20230003' THEN 12
        WHEN '20230004' THEN 8
        WHEN '20230005' THEN 14
        WHEN '20230008' THEN 17
        WHEN '20230009' THEN 9
        WHEN '20230010' THEN 7
        WHEN '20230011' THEN 13
        WHEN '20230012' THEN 9
        ELSE 0
    END AS review_count,
    CASE u.student_no
        WHEN '20230001' THEN 5.00
        WHEN '20230002' THEN 4.90
        WHEN '20230003' THEN 4.70
        WHEN '20230004' THEN 4.50
        WHEN '20230005' THEN 4.80
        WHEN '20230008' THEN 4.80
        WHEN '20230009' THEN 4.60
        WHEN '20230010' THEN 4.90
        WHEN '20230011' THEN 4.80
        WHEN '20230012' THEN 4.50
        ELSE 0.00
    END AS avg_rating,
    CASE u.student_no
        WHEN '20230001' THEN 1.0000
        WHEN '20230002' THEN 0.9600
        WHEN '20230003' THEN 0.9200
        WHEN '20230004' THEN 0.8800
        WHEN '20230005' THEN 0.9300
        WHEN '20230008' THEN 0.9400
        WHEN '20230009' THEN 0.8900
        WHEN '20230010' THEN 1.0000
        WHEN '20230011' THEN 0.9200
        WHEN '20230012' THEN 0.8900
        ELSE 0.0000
    END AS good_review_rate,
    CASE u.student_no
        WHEN '20230004' THEN 1
        WHEN '20230009' THEN 1
        WHEN '20230012' THEN 1
        ELSE 0
    END AS bad_review_count,
    1 AS version,
    NOW() AS calculated_at
FROM `user` u
WHERE u.student_no IN (
    'admin001',
    '20230001',
    '20230002',
    '20230003',
    '20230004',
    '20230005',
    '20230006',
    '20230007',
    '20230008',
    '20230009',
    '20230010',
    '20230011',
    '20230012'
)
ON DUPLICATE KEY UPDATE
    `user_id` = `user_id`;

-- =====================================================================
-- 3. 商品初始数据定义
-- =====================================================================
-- 临时表只在当前数据库连接中存在，执行结束后自动消失。
-- 把商品差异字段集中写在临时表里，后面的新增、更新、图片生成都复用同一份数据。
DROP TEMPORARY TABLE IF EXISTS tmp_seed_product;
CREATE TEMPORARY TABLE tmp_seed_product (
    `seller_student_no` VARCHAR(32) NOT NULL,
    `category_name` VARCHAR(30) NOT NULL,
    `title` VARCHAR(60) NOT NULL,
    `description` VARCHAR(2000) NOT NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `stock` INT NOT NULL,
    `item_condition` TINYINT NOT NULL,
    `campus` VARCHAR(30) NOT NULL,
    `trade_place` VARCHAR(60) NOT NULL,
    `status` TINYINT NOT NULL,
    `view_count` INT NOT NULL,
    `version` INT NOT NULL,
    `created_at` DATETIME NOT NULL,
    `image_keyword` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`seller_student_no`, `title`)
) ENGINE=InnoDB;

-- 商品状态：
--   0 草稿 / 1 待审核 / 2 审核驳回 / 3 在售 / 4 已下架 / 5 已售罄
-- 商品成色：
--   0 全新 / 1 几乎全新 / 2 轻微使用 / 3 明显使用
--
-- 前 24 条是校内公开市场商品，覆盖六个分类、四个校区和多个价格区间。
-- 最后 6 条归属账号 20230001。商品状态自然分布，便于长期覆盖完整业务流程。
INSERT INTO tmp_seed_product (
    `seller_student_no`,
    `category_name`,
    `title`,
    `description`,
    `price`,
    `stock`,
    `item_condition`,
    `campus`,
    `trade_place`,
    `status`,
    `view_count`,
    `version`,
    `created_at`,
    `image_keyword`
)
VALUES
    ('20230002', '数码电子', '95新 iPad Air 5 64G 深空灰',
     '2024 年购入，主要用于上课记笔记。屏幕无划痕，边框有一处轻微磕碰，电池健康正常。附原装充电器、保护套和替换笔尖，可当面验机。',
     2560.00, 1, 1, '东校区', '东校区三食堂门口', 3, 386, 1, '2026-07-24 18:26:00', 'ipad-tablet'),
    ('20230002', '数码电子', '罗技 MX Master 3S 无线鼠标',
     '自用约半年，静音按键、磁力滚轮和侧键均正常，Type-C 充电。外壳无明显油光，盒子和接收器都在。',
     350.00, 1, 1, '东校区', '东校区图书馆门口', 3, 312, 1, '2026-07-23 20:14:00', 'wireless-mouse'),
    ('20230012', '数码电子', '小米 Buds 4 Pro 降噪耳机',
     '购于小米之家，已过保。降噪、通透模式和双设备连接正常，充电盒有细小划痕，耳帽已清洁消毒。',
     238.00, 1, 2, '西校区', '西校区一号门', 3, 275, 1, '2026-07-22 12:08:00', 'wireless-earbuds'),
    ('20230011', '数码电子', 'Kindle Paperwhite 4 8G',
     '国行 8G 版本，墨水屏无坏点，背光均匀。机身背面有正常使用痕迹，附磁吸保护壳和数据线。',
     480.00, 1, 2, '东校区', '东校区实验楼大厅', 3, 198, 1, '2026-07-21 21:35:00', 'kindle-ebook-reader'),
    ('20230002', '数码电子', 'Keychron K2 蓝牙机械键盘 青轴',
     '87 键白光版，蓝牙和有线连接都正常，键帽无明显打油。因换了静音轴键盘出掉，原装线和拔键器齐全。',
     290.00, 1, 1, '北校区', '北校区体育馆', 1, 166, 1, '2026-07-20 16:42:00', 'mechanical-keyboard'),

    ('20230003', '图书教材', '大学英语四级真题 12 套',
     '星火英语 2025 年版，做过其中 4 套，听力音频二维码可用，答案册完整。三套一起拿可小刀。',
     18.00, 3, 2, '东校区', '东校区图书馆门口', 3, 142, 1, '2026-07-19 19:06:00', 'english-exam-books'),
    ('20230003', '图书教材', '《算法导论》第三版 中文',
     '机械工业出版社中文版，封面边角有轻微磨损，前五章有少量铅笔批注，内页完整无缺页。',
     55.00, 1, 2, '西校区', '西校区一号门', 3, 321, 1, '2026-07-18 13:27:00', 'algorithm-book'),
    ('20230003', '图书教材', '考研数学 张宇 1000 题',
     '2026 基础篇和强化篇两册，基础篇完成约三分之一，有正常书写痕迹，答案解析册未使用。',
     25.00, 1, 3, '南校区', '南校区二食堂', 3, 94, 1, '2026-07-17 09:48:00', 'mathematics-book'),
    ('20230011', '图书教材', '《深入理解计算机系统》CSAPP 中文版',
     '第三版中文版，买来后只翻过缓存和虚拟内存章节，内页干净，无写画和缺页。',
     68.00, 1, 1, '东校区', '东校区实验楼', 1, 223, 1, '2026-07-16 22:17:00', 'computer-science-book'),

    ('20230010', '生活用品', '欧普宿舍台灯 三档调光',
     'USB 供电，三档亮度，灯臂可以折叠。用了一个学期，灯珠无频闪，白色外壳有一点正常使用痕迹。',
     28.00, 1, 2, '南校区', '南校区宿舍区', 3, 87, 1, '2026-07-15 18:53:00', 'desk-lamp'),
    ('20230006', '生活用品', '不锈钢折叠晾衣架',
     '双杆落地款，展开约 1.4 米，可折叠靠墙收纳。连接处没有生锈，限校内自提。',
     35.00, 1, 2, '东校区', '东校区菜鸟驿站', 3, 109, 1, '2026-07-14 12:31:00', 'drying-rack'),
    ('20230012', '生活用品', '美的电热水壶 1.5L',
     '宿舍自用一学期，自动断电正常，壶内已除垢清洁。额定功率 1500W，请确认宿舍用电规定后购买。',
     32.00, 1, 2, '西校区', '西校区宿舍区', 3, 156, 1, '2026-07-13 20:02:00', 'electric-kettle'),
    ('20230006', '生活用品', '桌面收纳盒 多格文具整理',
     '奶白色塑料收纳盒，有六个分区，可放文具、遥控器和数据线。买多了两个，按个出售。',
     15.00, 2, 0, '东校区', '东校区二食堂', 3, 73, 1, '2026-07-12 14:46:00', 'desk-organizer'),

    ('20230005', '运动户外', '迪卡侬 篮球 7 号 室外耐磨',
     'Tarmak 7 号球，室外水泥场使用过五六次，表皮纹路清楚，不漏气，可在球场直接试球。',
     45.00, 1, 2, '南校区', '南校区篮球场', 3, 201, 1, '2026-07-11 17:24:00', 'basketball'),
    ('20230005', '运动户外', '瑜伽垫 加厚防滑 183cm',
     'NBR 10mm 加厚款，尺寸 183×61cm，附收纳绑带。使用次数不多，已擦拭清洁，无破损。',
     30.00, 1, 2, '南校区', '南校区体育馆', 1, 132, 1, '2026-07-10 11:39:00', 'yoga-mat'),
    ('20230009', '运动户外', '喜德盛 RC200 公路自行车',
     'S 码车架，适合约 165—175cm 身高。校内通勤一年，变速和刹车正常，车架有两处掉漆，支持现场试骑。',
     620.00, 1, 3, '北校区', '北校区一号门', 3, 418, 1, '2026-07-09 19:18:00', 'road-bicycle'),
    ('20230005', '运动户外', '羽毛球拍 尤尼克斯 单支',
     '入门碳素拍，4U G5，拉线约 24 磅。拍框没有裂纹，有一处小掉漆，刚换过手胶并附拍套。',
     88.00, 1, 2, '南校区', '南校区体育馆', 3, 177, 1, '2026-07-08 16:11:00', 'badminton-racket'),

    ('20230008', '服饰美妆', '优衣库男款摇粒绒外套 L 码',
     '深灰色 L 码，适合 170—178cm 左右。去年秋天购买，洗过两次，无污渍和明显起球。',
     58.00, 1, 2, '南校区', '南校区咖啡厅', 3, 118, 1, '2026-07-07 21:06:00', 'fleece-jacket'),
    ('20230008', '服饰美妆', '卡其色帆布双肩包 15寸',
     '大容量多隔层，可以放下 15 寸笔记本。只背过两次，拉链顺滑，肩带和内衬干净。',
     45.00, 1, 1, '南校区', '南校区宿舍区', 1, 96, 1, '2026-07-06 15:37:00', 'canvas-backpack'),
    ('20230012', '服饰美妆', 'Nike Dri-FIT 运动短袖 M 码',
     '黑色速干面料，M 码，衣长约 68cm。穿过三四次，无开线、无印花脱落，已清洗。',
     50.00, 1, 2, '西校区', '西校区体育馆', 3, 164, 1, '2026-07-05 12:52:00', 'sports-tshirt'),
    ('20230008', '服饰美妆', '品牌香水小样 3ml 五支',
     '专柜活动赠品，包含木质、花香和柑橘调，五支均未开封，适合先试香再买正装。',
     40.00, 1, 0, '南校区', '南校区咖啡厅', 3, 89, 1, '2026-07-04 20:23:00', 'perfume-samples'),

    ('20230010', '其他', '桌面多肉盆栽 含陶瓷盆',
     '宿舍窗台繁殖的小多肉，已经服盆，包含白色陶瓷盆。共有四盆，品种和大小略有区别，可现场挑选。',
     12.00, 4, 1, '南校区', '南校区二食堂', 3, 61, 1, '2026-07-03 10:16:00', 'succulent-plant'),
    ('20230004', '其他', '毕业季 打包出闲置一批',
     '毕业离校，台灯、插线板、衣架、收纳篮和小风扇一起出。均可正常使用，优先整套自提，单件价格可再商量。',
     99.00, 1, 3, '北校区', '北校区宿舍区', 3, 247, 1, '2026-07-02 18:41:00', 'dormitory-items'),
    ('20230007', '其他', '演唱会应援荧光棒 两支',
     '活动临时买的，两支只使用过一次，三档闪烁正常，附腕带，不含电池。',
     22.00, 1, 1, '西校区', '西校区一号门', 3, 79, 1, '2026-07-01 11:28:00', 'glow-sticks'),

    ('20230001', '数码电子', '雷柏 V500PRO 机械键盘 青轴',
     '104 键青轴白光版，键盘功能正常，空格键有轻微打油。因宿舍对声音比较敏感，准备换静音键盘。',
     128.00, 1, 2, '东校区', '东校区菜鸟驿站', 0, 10, 1, '2026-07-25 09:12:00', 'mechanical-keyboard'),
    ('20230001', '数码电子', 'HKC 24英寸 1080P 显示器',
     '型号 H249，IPS 面板，75Hz，无亮点和坏点。2024 年购买，含底座、电源线和一根 HDMI 线，只支持校内自提。',
     360.00, 1, 2, '东校区', '东校区菜鸟驿站', 1, 11, 1, '2026-07-24 09:37:00', 'computer-monitor'),
    ('20230001', '图书教材', '同济高等数学第七版 上下册',
     '上下两册一起出，上册前四章有少量课堂笔记，下册基本干净，封面和内页完整。',
     22.00, 1, 3, '东校区', '东校区图书馆门口', 2, 12, 1, '2026-07-23 09:45:00', 'calculus-books'),
    ('20230001', '生活用品', '米家台灯 Lite 白色',
     '三档色温和亮度调节正常，灯罩没有发黄，底座有一道不明显的划痕，附原装电源适配器。',
     49.00, 1, 1, '东校区', '东校区宿舍区', 3, 13, 1, '2026-07-22 09:28:00', 'desk-lamp'),
    ('20230001', '运动户外', '斯伯丁 7号 PU 篮球',
     '室内外通用款，使用约一个学期，表皮磨损不严重，不漏气。最近课程忙，很少打球所以出掉。',
     70.00, 1, 2, '东校区', '东校区篮球场', 4, 14, 1, '2026-07-21 09:03:00', 'basketball'),
    ('20230001', '服饰美妆', '安踏 狂潮5 低帮篮球鞋 42码',
     '黑白配色 42 码，实战穿过约十次，鞋底纹路和鞋面状态良好，鞋垫已清洗消毒，原盒还在。',
     330.00, 0, 2, '东校区', '东校区体育馆', 5, 15, 1, '2026-07-20 09:51:00', 'basketball-shoes');

-- =====================================================================
-- 4. 写入商品主表
-- =====================================================================
-- 只插入数据库中尚不存在的初始商品。
-- “卖家学号 + 商品标题”是本种子脚本的逻辑唯一标识，不要求修改正式表结构。
INSERT INTO product (
    `seller_id`,
    `category_id`,
    `title`,
    `description`,
    `price`,
    `stock`,
    `item_condition`,
    `campus`,
    `trade_place`,
    `status`,
    `view_count`,
    `version`,
    `deleted`,
    `created_at`
)
SELECT
    seller.id,
    category.id,
    seed.title,
    seed.description,
    seed.price,
    seed.stock,
    seed.item_condition,
    seed.campus,
    seed.trade_place,
    seed.status,
    seed.view_count,
    seed.version,
    0,
    seed.created_at
FROM tmp_seed_product seed
INNER JOIN `user` seller
    ON seller.student_no = seed.seller_student_no
INNER JOIN category
    ON category.name = seed.category_name
WHERE NOT EXISTS (
    SELECT 1
    FROM product existing_product
    WHERE existing_product.seller_id = seller.id
      AND existing_product.title = seed.title
);

-- =====================================================================
-- 5. 商品图片
-- =====================================================================
-- 每件商品配置三张与商品关键词相关的图片。
-- lock 参数由商品标题稳定计算，因此同一商品重复刷新时会得到相同图片。
-- 长期部署时建议把确认过的图片下载到自己的 /uploads 目录，避免依赖外部图片服务。
-- sort=0 的图片可作为商品列表封面，sort=1/2 用于详情页轮播。
INSERT INTO product_image (`product_id`, `url`, `sort`)
SELECT
    product.id,
    CONCAT(
        'https://loremflickr.com/600/450/',
        seed.image_keyword,
        '?lock=',
        MOD(CRC32(CONCAT(seed.title, '-', image_suffix.sort_value)), 100000)
    ) AS image_url,
    image_suffix.sort_value
FROM tmp_seed_product seed
INNER JOIN `user` seller
    ON seller.student_no = seed.seller_student_no
INNER JOIN product
    ON product.seller_id = seller.id
   AND product.title = seed.title
CROSS JOIN (
    SELECT 0 AS sort_value
    UNION ALL
    SELECT 1 AS sort_value
    UNION ALL
    SELECT 2 AS sort_value
) image_suffix
WHERE NOT EXISTS (
    SELECT 1
    FROM product_image existing_image
    WHERE existing_image.product_id = product.id
      AND existing_image.sort = image_suffix.sort_value
);

-- =====================================================================
-- 6. 审核记录
-- =====================================================================
-- product 表只保存当前状态，具体驳回原因应来自审核日志。
-- 为被驳回的教材补充真实审核原因，后续“我的商品”接口可据此返回 rejectReason。
INSERT INTO product_review_log (
    `product_id`,
    `reviewer_id`,
    `result`,
    `reason`,
    `created_at`
)
SELECT
    product.id,
    admin_user.id,
    2,
    '图片不清晰，请重新上传商品实拍图',
    '2026-07-24 14:30:00'
FROM product
INNER JOIN `user` seller
    ON seller.id = product.seller_id
CROSS JOIN `user` admin_user
WHERE seller.student_no = '20230001'
  AND product.title = '同济高等数学第七版 上下册'
  AND admin_user.student_no = 'admin001'
  AND NOT EXISTS (
      SELECT 1
      FROM product_review_log existing_log
      WHERE existing_log.product_id = product.id
        AND existing_log.result = 2
        AND existing_log.reason = '图片不清晰，请重新上传商品实拍图'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_seed_product;

COMMIT;

-- =====================================================================
-- 7. 执行结果检查
-- =====================================================================
-- 执行脚本后，IDEA 的结果窗口会显示以下统计，便于快速确认导入是否完整。
SELECT COUNT(*) AS seed_user_count
FROM `user`
WHERE student_no IN (
    'admin001',
    '20230001',
    '20230002',
    '20230003',
    '20230004',
    '20230005',
    '20230006',
    '20230007',
    '20230008',
    '20230009',
    '20230010',
    '20230011',
    '20230012'
);

SELECT
    status,
    CASE status
        WHEN 0 THEN '草稿'
        WHEN 1 THEN '待审核'
        WHEN 2 THEN '审核驳回'
        WHEN 3 THEN '在售'
        WHEN 4 THEN '已下架'
        WHEN 5 THEN '已售罄'
        ELSE '未知'
    END AS status_name,
    COUNT(*) AS product_count
FROM product
WHERE title IN (
    '95新 iPad Air 5 64G 深空灰',
    '罗技 MX Master 3S 无线鼠标',
    '小米 Buds 4 Pro 降噪耳机',
    'Kindle Paperwhite 4 8G',
    'Keychron K2 蓝牙机械键盘 青轴',
    '大学英语四级真题 12 套',
    '《算法导论》第三版 中文',
    '考研数学 张宇 1000 题',
    '《深入理解计算机系统》CSAPP 中文版',
    '欧普宿舍台灯 三档调光',
    '不锈钢折叠晾衣架',
    '美的电热水壶 1.5L',
    '桌面收纳盒 多格文具整理',
    '迪卡侬 篮球 7 号 室外耐磨',
    '瑜伽垫 加厚防滑 183cm',
    '喜德盛 RC200 公路自行车',
    '羽毛球拍 尤尼克斯 单支',
    '优衣库男款摇粒绒外套 L 码',
    '卡其色帆布双肩包 15寸',
    'Nike Dri-FIT 运动短袖 M 码',
    '品牌香水小样 3ml 五支',
    '桌面多肉盆栽 含陶瓷盆',
    '毕业季 打包出闲置一批',
    '演唱会应援荧光棒 两支',
    '雷柏 V500PRO 机械键盘 青轴',
    'HKC 24英寸 1080P 显示器',
    '同济高等数学第七版 上下册',
    '米家台灯 Lite 白色',
    '斯伯丁 7号 PU 篮球',
    '安踏 狂潮5 低帮篮球鞋 42码'
)
GROUP BY status
ORDER BY status;
