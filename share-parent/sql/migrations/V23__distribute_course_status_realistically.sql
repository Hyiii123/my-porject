-- V23: 真实化规范课程状态分布 (已上架 270、待上架 25、已下架 15、已完结 10，总计 320 门全 IT 课程)
-- 保证管理端课程管理顶部 5 大状态统计与底层数据库真实数据 100% 精确对齐

USE `tj_education`;

-- 1. 基础核心课程与大盘课程 (id 1 ~ 270)：保持已上架状态 (status = 1)
UPDATE `edu_course` SET `status` = 1, `publish_time` = IFNULL(`publish_time`, NOW()) WHERE `id` <= 270;

-- 2. 备课制作与预热阶段课程 (id 271 ~ 295)：设为待上架 (status = 0)
UPDATE `edu_course` SET `status` = 0, `publish_time` = NULL WHERE `id` BETWEEN 271 AND 295;

-- 3. 历史技术迭代过保课程 (id 296 ~ 310)：设为已下架 (status = 2)
UPDATE `edu_course` SET `status` = 2 WHERE `id` BETWEEN 296 AND 310;

-- 4. 专项训练营结课归档课程 (id 311 ~ 320)：设为已完结 (status = 3)
UPDATE `edu_course` SET `status` = 3 WHERE `id` BETWEEN 311 AND 320;
