-- 将早期演示种子中的用户编号 1~12 映射到 V10 创建的本地演示账号 101~112。
--
-- 说明：V7/V8 为了兼容原始 Mock 数据，最初使用了 1~12；V10 将演示账号
-- 放到 101~112，避免覆盖若依默认 admin 和既有系统用户。本脚本只按演示
-- 数据专用主键范围更新，并且限定旧用户编号，因此可以重复执行，也不会
-- 把真实业务用户的关联关系整体迁移。
SET NAMES utf8mb4;

USE `tj_education`;

UPDATE `edu_interest`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10301 AND 10310
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_learning_plan`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10401 AND 10408
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_learning_record`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10501 AND 10520
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_question`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10601 AND 10612
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_reply`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10701 AND 10718
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_question_like`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10801 AND 10810
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_note`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10901 AND 10912
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_note_collect`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10951 AND 10960
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_note_like`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 10971 AND 10980
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_exam_record`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 11301 AND 11312
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_sign_record`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 11501 AND 11516
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `edu_points_ledger`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 11601 AND 11621
  AND `user_id` BETWEEN 1 AND 12;

USE `tj_trade`;

UPDATE `mkt_user_coupon`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 12201 AND 12212
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `tr_cart`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 12301 AND 12308
  AND `user_id` BETWEEN 1 AND 12;

UPDATE `tr_order`
SET `user_id` = `user_id` + 100
WHERE `id` BETWEEN 12401 AND 12420
  AND `user_id` BETWEEN 1 AND 12;

-- 校验示例：
-- SELECT user_id, COUNT(*) FROM tj_education.edu_learning_record
-- WHERE id BETWEEN 10501 AND 10520 GROUP BY user_id;
-- SELECT user_id, COUNT(*) FROM tj_trade.tr_order
-- WHERE id BETWEEN 12401 AND 12420 GROUP BY user_id;
