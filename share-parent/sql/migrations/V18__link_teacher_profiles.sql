-- 将教育域教师资料与系统用户建立一对一关联。
-- 113~121 为本地演示教师账号，106~109 已由 V10 创建。
-- 脚本可重复执行，不覆盖已经存在的非空教师关联。
SET NAMES utf8mb4;

USE `share`;

INSERT IGNORE INTO `sys_user`
(`user_id`,`dept_id`,`user_name`,`nick_name`,`user_type`,`email`,`phonenumber`,`sex`,`avatar`,`password`,`status`,`del_flag`,`login_ip`,`create_by`,`create_time`,`remark`)
VALUES
(113,105,'teacher_chen','陈老师','02','chen.teacher@zhiwenxueban.com','13900139005','1','/profile/teacher/teacher5.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(114,105,'teacher_liu','刘老师','02','liu.teacher@zhiwenxueban.com','13900139006','0','/profile/teacher/teacher6.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(115,105,'teacher_yang','杨老师','02','yang.teacher@zhiwenxueban.com','13900139007','1','/profile/teacher/teacher7.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(116,105,'teacher_huang','黄老师','02','huang.teacher@zhiwenxueban.com','13900139008','0','/profile/teacher/teacher8.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(117,105,'teacher_zhou','周老师','02','zhou.teacher@zhiwenxueban.com','13900139009','1','/profile/teacher/teacher9.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(118,105,'teacher_wu','吴老师','02','wu.teacher@zhiwenxueban.com','13900139010','0','/profile/teacher/teacher10.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(119,105,'teacher_zheng','郑老师','02','zheng.teacher@zhiwenxueban.com','13900139011','1','/profile/teacher/teacher11.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(120,105,'teacher_sun','孙老师','02','sun.teacher@zhiwenxueban.com','13900139012','0','/profile/teacher/teacher12.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(121,105,'teacher_ma','马老师','02','ma.teacher@zhiwenxueban.com','13900139013','1','/profile/teacher/teacher13.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师');

INSERT IGNORE INTO `sys_user_role` (`user_id`,`role_id`) VALUES
(113,2),(114,2),(115,2),(116,2),(117,2),(118,2),(119,2),(120,2),(121,2);

-- 修复早期通过 Windows 代码页导入时可能产生的乱码昵称和备注。
UPDATE `sys_user` SET `nick_name` = '张老师', `remark` = '本地演示教师' WHERE `user_id` = 106;
UPDATE `sys_user` SET `nick_name` = '李老师', `remark` = '本地演示教师' WHERE `user_id` = 107;
UPDATE `sys_user` SET `nick_name` = '王老师', `remark` = '本地演示教师' WHERE `user_id` = 108;
UPDATE `sys_user` SET `nick_name` = '赵老师', `remark` = '本地演示教师' WHERE `user_id` = 109;
UPDATE `sys_user` SET `nick_name` = '陈老师', `remark` = '本地演示教师' WHERE `user_id` = 113;
UPDATE `sys_user` SET `nick_name` = '刘老师', `remark` = '本地演示教师' WHERE `user_id` = 114;
UPDATE `sys_user` SET `nick_name` = '杨老师', `remark` = '本地演示教师' WHERE `user_id` = 115;
UPDATE `sys_user` SET `nick_name` = '黄老师', `remark` = '本地演示教师' WHERE `user_id` = 116;
UPDATE `sys_user` SET `nick_name` = '周老师', `remark` = '本地演示教师' WHERE `user_id` = 117;
UPDATE `sys_user` SET `nick_name` = '吴老师', `remark` = '本地演示教师' WHERE `user_id` = 118;
UPDATE `sys_user` SET `nick_name` = '郑老师', `remark` = '本地演示教师' WHERE `user_id` = 119;
UPDATE `sys_user` SET `nick_name` = '孙老师', `remark` = '本地演示教师' WHERE `user_id` = 120;
UPDATE `sys_user` SET `nick_name` = '马老师', `remark` = '本地演示教师' WHERE `user_id` = 121;

USE `tj_education`;

UPDATE `edu_teacher` SET `user_id` = 106 WHERE `id` = 1 AND (`user_id` IS NULL OR `user_id` = 106);
UPDATE `edu_teacher` SET `user_id` = 107 WHERE `id` = 2 AND (`user_id` IS NULL OR `user_id` = 107);
UPDATE `edu_teacher` SET `user_id` = 108 WHERE `id` = 3 AND (`user_id` IS NULL OR `user_id` = 108);
UPDATE `edu_teacher` SET `user_id` = 109 WHERE `id` = 4 AND (`user_id` IS NULL OR `user_id` = 109);
UPDATE `edu_teacher` SET `user_id` = 113 WHERE `id` = 5 AND (`user_id` IS NULL OR `user_id` = 113);
UPDATE `edu_teacher` SET `user_id` = 114 WHERE `id` = 6 AND (`user_id` IS NULL OR `user_id` = 114);
UPDATE `edu_teacher` SET `user_id` = 115 WHERE `id` = 7 AND (`user_id` IS NULL OR `user_id` = 115);
UPDATE `edu_teacher` SET `user_id` = 116 WHERE `id` = 8 AND (`user_id` IS NULL OR `user_id` = 116);
UPDATE `edu_teacher` SET `user_id` = 117 WHERE `id` = 9 AND (`user_id` IS NULL OR `user_id` = 117);
UPDATE `edu_teacher` SET `user_id` = 118 WHERE `id` = 10 AND (`user_id` IS NULL OR `user_id` = 118);
UPDATE `edu_teacher` SET `user_id` = 119 WHERE `id` = 11 AND (`user_id` IS NULL OR `user_id` = 119);
UPDATE `edu_teacher` SET `user_id` = 120 WHERE `id` = 12 AND (`user_id` IS NULL OR `user_id` = 120);
UPDATE `edu_teacher` SET `user_id` = 121 WHERE `id` = 13 AND (`user_id` IS NULL OR `user_id` = 121);
