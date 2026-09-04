-- 客服会话生命周期修正：已有评价代表用户已结束本次服务。
-- 可重复执行；只补齐状态，不覆盖已经记录的关闭时间。
SET NAMES utf8mb4;
USE `tj_customer`;

UPDATE `cs_session` s
JOIN `cs_evaluation` e ON e.`session_id` = s.`id`
SET s.`status` = 3,
    s.`closed_at` = COALESCE(s.`closed_at`, e.`update_time`, e.`create_time`, CURRENT_TIMESTAMP),
    s.`updated_at` = CURRENT_TIMESTAMP,
    s.`update_time` = CURRENT_TIMESTAMP
WHERE s.`status` <> 3;
