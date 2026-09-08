-- V24: 将所有历史遗留的天机 (tianji) 数据与邮箱全面迁移对齐为智问 (zhiwen)
SET NAMES utf8mb4;
USE `share`;

-- 1. 更新所有学员与系统用户的邮箱域名为 @zhiwen.com
UPDATE `share`.`sys_user`
SET `email` = REPLACE(`email`, 'tianji.com', 'zhiwen.com')
WHERE `email` LIKE '%@tianji.com%';

-- 2. 检查并更新用户名或备注中若有遗留天机字样
UPDATE `share`.`sys_user`
SET `remark` = REPLACE(`remark`, '天机', '智问')
WHERE `remark` LIKE '%天机%';

-- 3. 检查并对齐 edu_course、edu_teacher 文案
USE `tj_education`;

UPDATE `tj_education`.`edu_teacher`
SET `teacher_name` = REPLACE(`teacher_name`, '天机', '智问'),
    `introduction` = REPLACE(`introduction`, '天机', '智问')
WHERE `teacher_name` LIKE '%天机%' OR `introduction` LIKE '%天机%';

UPDATE `tj_education`.`edu_course`
SET `course_name` = REPLACE(`course_name`, '天机', '智问'),
    `short_description` = REPLACE(`short_description`, '天机', '智问'),
    `description` = REPLACE(`description`, '天机', '智问')
WHERE `course_name` LIKE '%天机%' OR `short_description` LIKE '%天机%' OR `description` LIKE '%天机%';
