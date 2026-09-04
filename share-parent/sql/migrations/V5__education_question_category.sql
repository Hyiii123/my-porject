-- 为旧题库管理页补充课程分类关联。生产环境由 Flyway 执行；重复执行安全。
SET NAMES utf8mb4;
USE `tj_education`;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'edu_exam_question_bank'
    AND column_name = 'category_id'
);
SET @add_column_sql := IF(@column_exists = 0,
  'ALTER TABLE edu_exam_question_bank ADD COLUMN category_id bigint DEFAULT NULL COMMENT ''课程分类 ID，兼容旧题库分类筛选'' AFTER id',
  'SELECT 1');
PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @index_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'edu_exam_question_bank'
    AND index_name = 'idx_bank_category'
);
SET @add_index_sql := IF(@index_exists = 0,
  'ALTER TABLE edu_exam_question_bank ADD INDEX idx_bank_category (category_id)',
  'SELECT 1');
PREPARE add_index_stmt FROM @add_index_sql;
EXECUTE add_index_stmt;
DEALLOCATE PREPARE add_index_stmt;
