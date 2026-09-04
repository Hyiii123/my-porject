-- 天机学堂：交易与营销数据库（MySQL 8.x）
-- 数据边界：share-trade
-- 说明：user_id、course_id、coupon_id 是逻辑关联，不建立跨服务外键。
-- 金额统一按元保存；原 Mock 中金额是分，迁移数据已换算为元。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `tj_trade`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `tj_trade`;

CREATE TABLE IF NOT EXISTS `mkt_coupon` (
  `id` bigint NOT NULL,
  `coupon_name` varchar(200) NOT NULL,
  `coupon_type` tinyint NOT NULL DEFAULT 1 COMMENT '1普通，2VIP',
  `discount_type` tinyint NOT NULL COMMENT '2折扣，3无门槛减，4满减',
  `discount_value` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '折扣或优惠金额',
  `threshold_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `max_discount_amount` decimal(12,2) DEFAULT NULL,
  `total_count` int NOT NULL DEFAULT 0,
  `received_count` int NOT NULL DEFAULT 0,
  `used_count` int NOT NULL DEFAULT 0,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用，1可用，2结束',
  `description` varchar(1000) DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_legacy_id` (`legacy_id`),
  KEY `idx_coupon_status_time` (`status`, `start_time`, `end_time`),
  KEY `idx_coupon_type` (`coupon_type`, `discount_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS `mkt_coupon_code` (
  `id` bigint NOT NULL,
  `coupon_id` bigint NOT NULL,
  `coupon_code` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0未使用，1已兑换，2失效',
  `exchanged_user_id` bigint DEFAULT NULL,
  `exchanged_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_coupon_code_coupon` (`coupon_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券兑换码';

CREATE TABLE IF NOT EXISTS `mkt_user_coupon` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `coupon_id` bigint NOT NULL,
  `source_type` varchar(32) NOT NULL DEFAULT 'receive' COMMENT 'receive/exchange/activity',
  `receive_code` varchar(64) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0未使用，1已使用，2已过期，3已作废',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_at` datetime DEFAULT NULL,
  `used_at` datetime DEFAULT NULL,
  `used_order_id` bigint DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon_legacy_id` (`legacy_id`),
  UNIQUE KEY `uk_user_coupon_receive_code` (`receive_code`),
  KEY `idx_user_coupon_user_status` (`user_id`, `status`, `expire_at`),
  KEY `idx_user_coupon_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

CREATE TABLE IF NOT EXISTS `tr_cart` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `course_name` varchar(200) DEFAULT NULL COMMENT '加购时课程名称快照',
  `course_cover_url` varchar(500) DEFAULT NULL COMMENT '加购时课程封面快照',
  `teacher_name` varchar(100) DEFAULT NULL COMMENT '加购时讲师名称快照',
  `unit_price` decimal(12,2) DEFAULT NULL COMMENT '加购时课程价格快照，单位元',
  `quantity` int NOT NULL DEFAULT 1,
  `selected` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `active_user_id` bigint GENERATED ALWAYS AS (IF(`del_flag` = 0, `user_id`, NULL)) STORED COMMENT '仅有效购物车参与唯一约束',
  `active_course_id` bigint GENERATED ALWAYS AS (IF(`del_flag` = 0, `course_id`, NULL)) STORED COMMENT '仅有效购物车参与唯一约束',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_active_user_course` (`active_user_id`, `active_course_id`),
  KEY `idx_cart_user_selected` (`user_id`, `selected`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

CREATE TABLE IF NOT EXISTS `tr_order` (
  `id` bigint NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `total_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `discount_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `payable_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `paid_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `coupon_id` bigint DEFAULT NULL,
  `order_status` tinyint NOT NULL DEFAULT 0 COMMENT '0待支付，1已完成，2待支付超时，3已退款，4已取消',
  `payment_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未支付，1已支付，2支付失败，3已退款',
  `payment_channel` varchar(32) DEFAULT NULL COMMENT 'alipay/wechat/balance',
  `expire_time` datetime DEFAULT NULL,
  `paid_time` datetime DEFAULT NULL,
  `refund_time` datetime DEFAULT NULL,
  `refund_reason` varchar(500) DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_order_legacy_id` (`legacy_id`),
  KEY `idx_order_user_time` (`user_id`, `create_time`),
  KEY `idx_order_status_time` (`order_status`, `create_time`),
  KEY `idx_order_payment_status` (`payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

CREATE TABLE IF NOT EXISTS `tr_order_item` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `course_name` varchar(200) NOT NULL COMMENT '下单时课程名称快照',
  `course_cover_url` varchar(500) DEFAULT NULL,
  `unit_price` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '成交前单价快照',
  `discount_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `payable_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `quantity` int NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order` (`order_id`),
  KEY `idx_order_item_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

CREATE TABLE IF NOT EXISTS `tr_payment_order` (
  `id` bigint NOT NULL,
  `payment_no` varchar(64) NOT NULL,
  `order_id` bigint NOT NULL,
  `payment_channel` varchar(32) NOT NULL,
  `amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待支付，1成功，2失败，3关闭',
  `third_party_no` varchar(128) DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `paid_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_payment_channel_order` (`order_id`, `payment_channel`),
  KEY `idx_payment_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单';

CREATE TABLE IF NOT EXISTS `tr_refund_apply` (
  `id` bigint NOT NULL,
  `refund_no` varchar(64) NOT NULL,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `refund_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `reason` varchar(500) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待审核，1通过，2拒绝，3已退款',
  `audit_user_id` bigint DEFAULT NULL,
  `audit_remark` varchar(500) DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `refunded_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT 0,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_refund_order` (`order_id`),
  KEY `idx_refund_user_status` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请';

CREATE TABLE IF NOT EXISTS `tr_order_status_log` (
  `id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `from_status` tinyint DEFAULT NULL,
  `to_status` tinyint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_type` varchar(32) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_status_log_order_time` (`order_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变更日志';

-- 当前前端 coupon.json 的 10 条初始化优惠券。
INSERT INTO `mkt_coupon`
(`id`, `coupon_name`, `coupon_type`, `discount_type`, `discount_value`, `threshold_amount`, `max_discount_amount`, `total_count`, `used_count`, `start_time`, `end_time`, `status`, `description`, `legacy_id`)
VALUES
(1, '新用户专享券', 1, 3, 50.00, 0.00, 50.00, 1000, 456, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '新用户注册即可领取，无门槛使用', '1'),
(2, '满减优惠券', 1, 4, 100.00, 200.00, 100.00, 500, 234, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '满200元减10元', '2'),
(3, '折扣优惠券', 1, 2, 8.00, 100.00, 500.00, 300, 128, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '满100元打8折，最高优惠50元', '3'),
(4, '春季特惠券', 1, 4, 200.00, 500.00, 200.00, 200, 89, '2024-03-01 00:00:00', '2024-05-31 23:59:59', 1, '春季限时优惠，满500元减20元', '4'),
(5, '课程体验券', 1, 3, 30.00, 0.00, 30.00, 2000, 1256, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '课程体验专用，无门槛使用', '5'),
(6, 'VIP专属券', 2, 4, 500.00, 1000.00, 500.00, 100, 45, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, 'VIP会员专属，满1000元减50元', '6'),
(7, '端午节特惠', 1, 4, 150.00, 300.00, 150.00, 500, 0, '2024-06-01 00:00:00', '2024-06-30 23:59:59', 1, '端午节限时优惠', '7'),
(8, '暑期学习券', 1, 3, 100.00, 0.00, 100.00, 1000, 0, '2024-07-01 00:00:00', '2024-08-31 23:59:59', 1, '暑期学习专享，无门槛使用', '8'),
(9, '双十一狂欢券', 1, 4, 1000.00, 2000.00, 1000.00, 500, 0, '2024-11-01 00:00:00', '2024-11-11 23:59:59', 1, '双十一限时大促', '9'),
(10, '年终回馈券', 1, 2, 7.00, 200.00, 1000.00, 300, 0, '2024-12-01 00:00:00', '2024-12-31 23:59:59', 1, '年终大促，满200元打7折', '10')
ON DUPLICATE KEY UPDATE
`coupon_name` = VALUES(`coupon_name`), `discount_value` = VALUES(`discount_value`),
`threshold_amount` = VALUES(`threshold_amount`), `max_discount_amount` = VALUES(`max_discount_amount`),
`total_count` = VALUES(`total_count`), `used_count` = VALUES(`used_count`),
`status` = VALUES(`status`), `description` = VALUES(`description`);

-- 校验示例：SELECT COUNT(*) FROM mkt_coupon WHERE del_flag = 0;
-- 预期当前静态优惠券数量为 10。订单和学习记录将在迁移程序中按原始订单号导入。
