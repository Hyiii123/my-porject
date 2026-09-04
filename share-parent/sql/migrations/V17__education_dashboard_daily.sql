-- 将工作台统计从前端演示常量迁移为教育服务数据库快照。
-- 日期相对 CURRENT_DATE 生成，脚本可重复执行，不删除业务数据。
SET NAMES utf8mb4;
USE `tj_education`;

CREATE TABLE IF NOT EXISTS `edu_dashboard_daily` (
  `id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `visits` bigint NOT NULL DEFAULT 0 COMMENT '访问量',
  `order_count` bigint NOT NULL DEFAULT 0 COMMENT '订单数',
  `order_revenue` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单收入，单位元',
  `new_students` bigint NOT NULL DEFAULT 0 COMMENT '新增学员数',
  `active_users` bigint NOT NULL DEFAULT 0 COMMENT '日活跃用户数',
  `total_students` bigint NOT NULL DEFAULT 0 COMMENT '累计学员数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dashboard_daily_date` (`stat_date`),
  KEY `idx_dashboard_daily_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教育工作台日统计';

INSERT INTO `edu_dashboard_daily`
(`id`, `stat_date`, `visits`, `order_count`, `order_revenue`, `new_students`, `active_users`, `total_students`)
VALUES
(11701, DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY), 9480, 88, 14680.00, 62, 5840, 49780),
(11702, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), 10320, 104, 18260.00, 74, 6420, 49854),
(11703, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), 11860, 121, 21580.00, 81, 7050, 49935),
(11704, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), 11040, 96, 17240.00, 69, 6880, 50004),
(11705, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 13280, 143, 24980.00, 92, 7990, 50096),
(11706, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 14560, 152, 27180.00, 87, 8650, 50183),
(11707, CURRENT_DATE, 15820, 167, 29650.00, 96, 9230, 50279)
ON DUPLICATE KEY UPDATE
`visits` = VALUES(`visits`), `order_count` = VALUES(`order_count`),
`order_revenue` = VALUES(`order_revenue`), `new_students` = VALUES(`new_students`),
`active_users` = VALUES(`active_users`), `total_students` = VALUES(`total_students`);
