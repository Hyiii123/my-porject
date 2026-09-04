-- 客服管理菜单只允许超级管理员使用。
-- 早期客服菜单脚本曾把菜单授权给 common 角色；本脚本兼容已初始化数据库并可重复执行。
SET NAMES utf8mb4;
USE `share`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1,2200),(1,2201),(1,2202),(1,2203),(1,2204),(1,2205),(1,2206),(1,2207),(1,2208),(1,2209),
(1,2210),(1,2211),(1,2212),(1,2213),(1,2214),(1,2215);

DELETE FROM `sys_role_menu`
WHERE `role_id` = 2
  AND `menu_id` BETWEEN 2200 AND 2215;
