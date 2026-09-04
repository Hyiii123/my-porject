-- 教育旧接口兼容字段：支持课程媒资名称、试看标记及小节题目关联。
SET NAMES utf8mb4;
USE `tj_education`;

SET @media_name_sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `edu_course_catalog` ADD COLUMN `media_name` varchar(255) DEFAULT NULL COMMENT ''媒资名称快照'' AFTER `media_id`',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'edu_course_catalog' AND column_name = 'media_name'
);
PREPARE stmt FROM @media_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @trailer_sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `edu_course_catalog` ADD COLUMN `trailer` tinyint NOT NULL DEFAULT 0 COMMENT ''是否允许试看'' AFTER `is_free`',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'edu_course_catalog' AND column_name = 'trailer'
);
PREPARE stmt FROM @trailer_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `edu_catalog_question` (
  `id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `catalog_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `sort_num` int NOT NULL DEFAULT 0,
  `score` decimal(8,2) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_question` (`catalog_id`, `question_id`),
  KEY `idx_catalog_question_course` (`course_id`, `catalog_id`, `sort_num`),
  KEY `idx_catalog_question_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程小节题目关联';
