-- 系统用户与天机业务端演示数据。
-- 统一使用底座的 BCrypt 密码哈希；仅用于本地演示账号，生产环境请改为真实用户并轮换密码。
SET NAMES utf8mb4;
USE `share`;

INSERT IGNORE INTO `sys_user`
(`user_id`,`dept_id`,`user_name`,`nick_name`,`user_type`,`email`,`phonenumber`,`sex`,`avatar`,`password`,`status`,`del_flag`,`login_ip`,`create_by`,`create_time`,`remark`)
VALUES
(101,105,'student_zhang','张同学','01','zhang.student@zhiwenxueban.com','13800138001','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示学员'),
(102,105,'student_li','李同学','01','li.student@zhiwenxueban.com','13800138002','0','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示学员'),
(103,105,'student_wang','王同学','01','wang.student@zhiwenxueban.com','13800138003','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示学员'),
(104,105,'student_zhao','赵同学','01','zhao.student@zhiwenxueban.com','13800138004','0','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','1','0','','admin',NOW(),'本地演示学员'),
(105,105,'student_qian','钱同学','01','qian.student@zhiwenxueban.com','13800138005','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示学员'),
(106,105,'teacher_zhang','张老师','02','zhang.teacher@zhiwenxueban.com','13900139001','1','/profile/teacher/teacher1.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(107,105,'teacher_li','李老师','02','li.teacher@zhiwenxueban.com','13900139002','0','/profile/teacher/teacher2.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(108,105,'teacher_wang','王老师','02','wang.teacher@zhiwenxueban.com','13900139003','1','/profile/teacher/teacher3.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(109,105,'teacher_zhao','赵老师','02','zhao.teacher@zhiwenxueban.com','13900139004','0','/profile/teacher/teacher4.svg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示教师'),
(110,105,'operator_one','运营小明','03','operator.one@zhiwenxueban.com','13700137001','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示运营人员'),
(111,105,'operator_two','运营小红','03','operator.two@zhiwenxueban.com','13700137002','0','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示运营人员'),
(112,105,'student_chen','陈同学','01','chen.student@zhiwenxueban.com','13800138006','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','','admin',NOW(),'本地演示学员');

INSERT IGNORE INTO `sys_user_role` (`user_id`,`role_id`) VALUES
(101,2),(102,2),(103,2),(104,2),(105,2),(106,2),(107,2),(108,2),(109,2),(110,2),(111,2),(112,2);
