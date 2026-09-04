-- 修复购物车逻辑删除后的唯一索引冲突。
-- 旧索引 uk_cart_user_course(user_id, course_id, del_flag) 会把所有已删除记录
-- 也限制为同一用户/课程只能有一条，导致再次加购已删除课程时插入失败。
-- 生成列在 del_flag=1 时为 NULL，MySQL 唯一索引允许多条 NULL，因而只约束有效购物车。
-- 可重复执行，不删除历史购物车记录。
SET NAMES utf8mb4;
USE `tj_trade`;

SET @index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tr_cart'
      AND index_name = 'uk_cart_user_course'
);
SET @alter_sql := IF(@index_exists > 0,
    'ALTER TABLE `tr_cart` DROP INDEX `uk_cart_user_course`',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tr_cart'
      AND column_name = 'active_user_id'
);
SET @alter_sql := IF(@column_exists = 0,
    'ALTER TABLE `tr_cart` ADD COLUMN `active_user_id` bigint GENERATED ALWAYS AS (IF(`del_flag` = 0, `user_id`, NULL)) STORED COMMENT ''仅有效购物车参与唯一约束'' AFTER `del_flag`',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tr_cart'
      AND column_name = 'active_course_id'
);
SET @alter_sql := IF(@column_exists = 0,
    'ALTER TABLE `tr_cart` ADD COLUMN `active_course_id` bigint GENERATED ALWAYS AS (IF(`del_flag` = 0, `course_id`, NULL)) STORED COMMENT ''仅有效购物车参与唯一约束'' AFTER `active_user_id`',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'tr_cart'
      AND index_name = 'uk_cart_active_user_course'
);
SET @alter_sql := IF(@index_exists = 0,
    'ALTER TABLE `tr_cart` ADD UNIQUE KEY `uk_cart_active_user_course` (`active_user_id`, `active_course_id`)',
    'SELECT 1'
);
PREPARE alter_stmt FROM @alter_sql;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

