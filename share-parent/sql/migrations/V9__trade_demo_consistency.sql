-- 交易演示数据一致性校正。
-- 用于已经导入过旧版 V8 的数据卷；仅修正带 demo 标识的演示记录。
SET NAMES utf8mb4;
USE `tj_trade`;

UPDATE `mkt_user_coupon`
SET `used_order_id` = CASE `id`
  WHEN 12201 THEN 12401
  WHEN 12203 THEN 12403
  WHEN 12204 THEN 12404
  WHEN 12206 THEN 12406
  WHEN 12209 THEN 12409
  ELSE `used_order_id`
END
WHERE `id` IN (12201, 12203, 12204, 12206, 12209)
  AND `legacy_id` LIKE 'demo:user-coupon:%';

UPDATE `tr_order_item`
SET `unit_price` = CASE `id`
  WHEN 12505 THEN 499.00
  WHEN 12508 THEN 399.00
  WHEN 12518 THEN 349.00
  WHEN 12519 THEN 199.00
  WHEN 12520 THEN 499.00
  ELSE `unit_price`
END
WHERE `id` IN (12505, 12508, 12518, 12519, 12520);
