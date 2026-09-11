-- 媒资管理与课程目录小节双向绑定对齐
-- 为 tj_file.file_media 新增 course_id、course_name、section_id、section_name 字段及联合索引
-- 并回填当前已被课程小节引用的历史媒资数据

SET NAMES utf8mb4;

USE `tj_file`;

-- 1. 为 file_media 新增课程与小节字段（如果不存在）
SET @col_course_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'tj_file' AND TABLE_NAME = 'file_media' AND COLUMN_NAME = 'course_id');
SET @sql_course_id := IF(@col_course_id = 0, 'ALTER TABLE `tj_file`.`file_media` ADD COLUMN `course_id` bigint DEFAULT NULL COMMENT \'关联课程ID\' AFTER `description`', 'SELECT 1');
PREPARE stmt1 FROM @sql_course_id;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @col_course_name := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'tj_file' AND TABLE_NAME = 'file_media' AND COLUMN_NAME = 'course_name');
SET @sql_course_name := IF(@col_course_name = 0, 'ALTER TABLE `tj_file`.`file_media` ADD COLUMN `course_name` varchar(255) DEFAULT NULL COMMENT \'关联课程名称\' AFTER `course_id`', 'SELECT 1');
PREPARE stmt2 FROM @sql_course_name;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @col_section_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'tj_file' AND TABLE_NAME = 'file_media' AND COLUMN_NAME = 'section_id');
SET @sql_section_id := IF(@col_section_id = 0, 'ALTER TABLE `tj_file`.`file_media` ADD COLUMN `section_id` bigint DEFAULT NULL COMMENT \'关联小节ID(对应edu_course_catalog.id)\' AFTER `course_name`', 'SELECT 1');
PREPARE stmt3 FROM @sql_section_id;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SET @col_section_name := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'tj_file' AND TABLE_NAME = 'file_media' AND COLUMN_NAME = 'section_name');
SET @sql_section_name := IF(@col_section_name = 0, 'ALTER TABLE `tj_file`.`file_media` ADD COLUMN `section_name` varchar(255) DEFAULT NULL COMMENT \'关联小节名称(第几节)\' AFTER `section_id`', 'SELECT 1');
PREPARE stmt4 FROM @sql_section_name;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

-- 2. 添加联合索引（如果不存在）
SET @idx_count := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'tj_file' AND TABLE_NAME = 'file_media' AND INDEX_NAME = 'idx_file_media_course_section');
SET @sql_idx := IF(@idx_count = 0, 'ALTER TABLE `tj_file`.`file_media` ADD INDEX `idx_file_media_course_section` (`course_id`, `section_id`)', 'SELECT 1');
PREPARE stmt5 FROM @sql_idx;
EXECUTE stmt5;
DEALLOCATE PREPARE stmt5;

-- 3. 数据对齐：将 tj_education.edu_course_catalog 中已引用的媒资关系对齐回填至 tj_file.file_media
UPDATE `tj_file`.`file_media` m
INNER JOIN `tj_education`.`edu_course_catalog` c ON m.id = c.media_id
INNER JOIN `tj_education`.`edu_course` co ON c.course_id = co.id
SET m.course_id = c.course_id,
    m.course_name = co.course_name,
    m.section_id = c.id,
    m.section_name = c.catalog_title,
    m.status = 'used',
    m.media_type = 'course'
WHERE c.media_id IS NOT NULL;
