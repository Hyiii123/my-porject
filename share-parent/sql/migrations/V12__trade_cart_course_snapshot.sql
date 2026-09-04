-- 购物车课程快照与教育服务 Feign 适配。
-- 可重复执行，不删除原有购物车和订单数据。
SET NAMES utf8mb4;
USE `tj_trade`;

-- 兼容 MySQL 8.0 的不同小版本：部分版本不支持 ADD COLUMN IF NOT EXISTS，
-- 因此逐列检查 information_schema 后再执行 DDL。
SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tr_cart' AND column_name = 'course_name'
);
SET @alter_sql := IF(@column_exists = 0,
    'ALTER TABLE `tr_cart` ADD COLUMN `course_name` varchar(200) DEFAULT NULL COMMENT ''加购时课程名称快照'' AFTER `course_id`',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tr_cart' AND column_name = 'course_cover_url'
);
SET @alter_sql := IF(@column_exists = 0,
    'ALTER TABLE `tr_cart` ADD COLUMN `course_cover_url` varchar(500) DEFAULT NULL COMMENT ''加购时课程封面快照'' AFTER `course_name`',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tr_cart' AND column_name = 'teacher_name'
);
SET @alter_sql := IF(@column_exists = 0,
    'ALTER TABLE `tr_cart` ADD COLUMN `teacher_name` varchar(100) DEFAULT NULL COMMENT ''加购时讲师名称快照'' AFTER `course_cover_url`',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tr_cart' AND column_name = 'unit_price'
);
SET @alter_sql := IF(@column_exists = 0,
    'ALTER TABLE `tr_cart` ADD COLUMN `unit_price` decimal(12,2) DEFAULT NULL COMMENT ''加购时课程价格快照，单位元'' AFTER `teacher_name`',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

-- 当前各业务库位于同一个 MySQL 实例时，为历史购物车补齐可读快照。
-- 若教育库尚未初始化，本更新不会影响购物车主记录，服务层会通过 Feign 再尝试读取。
UPDATE `tr_cart` c
JOIN `tj_education`.`edu_course` e ON e.`id` = c.`course_id`
SET c.`course_name` = COALESCE(c.`course_name`, e.`course_name`),
    c.`course_cover_url` = COALESCE(c.`course_cover_url`, e.`cover_url`),
    c.`unit_price` = COALESCE(c.`unit_price`, e.`price`)
WHERE c.`course_name` IS NULL OR c.`course_cover_url` IS NULL OR c.`unit_price` IS NULL;
