-- V31: 清理 edu_course 课程简介字段中的 HTML 原始标签，重置为纯净文本
USE `tj_education`;

-- 1. 将包含 HTML 标签的课程简介更新为纯文本 short_description
UPDATE `edu_course` 
SET `description` = `short_description` 
WHERE `description` LIKE '%<%>%';

-- 2. 将为 NULL 的课程简介补全为 short_description
UPDATE `edu_course` 
SET `description` = `short_description` 
WHERE `description` IS NULL;
