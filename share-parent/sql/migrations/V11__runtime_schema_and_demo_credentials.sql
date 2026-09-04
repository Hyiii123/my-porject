-- 修复已初始化数据库的运行时兼容问题，并统一本地演示账号密码。
-- 该脚本可重复执行；不删除业务数据。
SET NAMES utf8mb4;

USE `tj_education`;

SET @has_version := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'edu_course_recommend'
      AND column_name = 'version'
);
SET @add_version_sql := IF(
    @has_version = 0,
    'ALTER TABLE `edu_course_recommend` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT ''乐观锁版本'' AFTER `del_flag`',
    'SELECT 1'
);
PREPARE add_version_stmt FROM @add_version_sql;
EXECUTE add_version_stmt;
DEALLOCATE PREPARE add_version_stmt;

USE `share`;

UPDATE `sys_user`
-- BCrypt("admin123")，与若依默认演示账号保持一致。
SET `password` = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2'
WHERE `user_name` IN (
    'admin', 'student_zhang', 'student_li', 'student_wang', 'student_zhao',
    'student_qian', 'teacher_zhang', 'teacher_li', 'teacher_wang', 'teacher_zhao',
    'operator_one', 'operator_two', 'student_chen'
);
