-- 客服管理菜单增量迁移，可重复执行。
-- 仅增加 share 库中的菜单与普通角色授权，不修改、不删除原菜单。
SET NAMES utf8mb4;
USE `share`;

INSERT INTO `sys_menu`
(`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`)
VALUES
(2200,'客服管理',0,4,'customer',NULL,'',1,0,'M','0','0','','message','admin',NOW(),'',NULL, 'AI客服管理目录'),
(2201,'客服工作台',2200,1,'management','customer/management/index',NULL,1,0,'C','0','0','customer:session:list','message','admin',NOW(),'',NULL, '客服知识库、会话和统计管理'),
(2202,'知识库查询',2201,1,'#','',NULL,1,0,'F','0','0','customer:knowledge:list','#','admin',NOW(),'',NULL, ''),
(2203,'知识库新增',2201,2,'#','',NULL,1,0,'F','0','0','customer:knowledge:add','#','admin',NOW(),'',NULL, ''),
(2204,'知识库修改',2201,3,'#','',NULL,1,0,'F','0','0','customer:knowledge:edit','#','admin',NOW(),'',NULL, ''),
(2205,'知识库删除',2201,4,'#','',NULL,1,0,'F','0','0','customer:knowledge:remove','#','admin',NOW(),'',NULL, ''),
(2206,'常见问题查询',2201,5,'#','',NULL,1,0,'F','0','0','customer:faq:list','#','admin',NOW(),'',NULL, ''),
(2207,'常见问题新增',2201,6,'#','',NULL,1,0,'F','0','0','customer:faq:add','#','admin',NOW(),'',NULL, ''),
(2208,'常见问题修改',2201,7,'#','',NULL,1,0,'F','0','0','customer:faq:edit','#','admin',NOW(),'',NULL, ''),
(2209,'常见问题删除',2201,8,'#','',NULL,1,0,'F','0','0','customer:faq:remove','#','admin',NOW(),'',NULL, ''),
(2210,'会话查询',2201,9,'#','',NULL,1,0,'F','0','0','customer:session:query','#','admin',NOW(),'',NULL, ''),
(2211,'会话关闭',2201,10,'#','',NULL,1,0,'F','0','0','customer:session:close','#','admin',NOW(),'',NULL, ''),
(2212,'统计查看',2201,11,'#','',NULL,1,0,'F','0','0','customer:statistics:view','#','admin',NOW(),'',NULL, ''),
(2213,'AI配置查询',2201,12,'#','',NULL,1,0,'F','0','0','customer:ai:query','#','admin',NOW(),'',NULL, ''),
(2214,'AI配置修改',2201,13,'#','',NULL,1,0,'F','0','0','customer:ai:edit','#','admin',NOW(),'',NULL, ''),
(2215,'AI连通性测试',2201,14,'#','',NULL,1,0,'F','0','0','customer:ai:test','#','admin',NOW(),'',NULL, '')
ON DUPLICATE KEY UPDATE
`menu_name`=VALUES(`menu_name`),`parent_id`=VALUES(`parent_id`),`order_num`=VALUES(`order_num`),
`path`=VALUES(`path`),`component`=VALUES(`component`),`perms`=VALUES(`perms`),`icon`=VALUES(`icon`),
`status`=VALUES(`status`),`visible`=VALUES(`visible`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
(1,2200),(1,2201),(1,2202),(1,2203),(1,2204),(1,2205),(1,2206),(1,2207),(1,2208),(1,2209),
(1,2210),(1,2211),(1,2212),(1,2213),(1,2214),(1,2215);
