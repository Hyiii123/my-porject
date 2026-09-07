-- V19: 为课程增加技能标签与难度标定，建立多维用户画像表。
-- 适用于基于 IT 课程数据集（MOOCCube/Coursera/Udemy）参数的智能推荐系统。
-- 该脚本遵循幂等原则，可安全重复执行。
SET NAMES utf8mb4;

USE `tj_education`;

-- 1. 动态增加 edu_course 推荐相关元数据字段
SET @has_difficulty := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'difficulty_level'
);
SET @sql_add_diff := IF(@has_difficulty = 0,
    'ALTER TABLE `edu_course` ADD COLUMN `difficulty_level` tinyint NOT NULL DEFAULT 2 COMMENT ''难度级别：1初级入门，2中级进阶，3高级架构'' AFTER `learner_count`',
    'SELECT 1'
);
PREPARE stmt_diff FROM @sql_add_diff;
EXECUTE stmt_diff;
DEALLOCATE PREPARE stmt_diff;

SET @has_skills := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'skills'
);
SET @sql_add_skills := IF(@has_skills = 0,
    'ALTER TABLE `edu_course` ADD COLUMN `skills` varchar(255) DEFAULT NULL COMMENT ''细粒度技术技能标签（逗号分隔）'' AFTER `difficulty_level`',
    'SELECT 1'
);
PREPARE stmt_skills FROM @sql_add_skills;
EXECUTE stmt_skills;
DEALLOCATE PREPARE stmt_skills;

SET @has_target_role := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'target_role'
);
SET @sql_add_role := IF(@has_target_role = 0,
    'ALTER TABLE `edu_course` ADD COLUMN `target_role` varchar(100) DEFAULT NULL COMMENT ''目标岗位角色'' AFTER `skills`',
    'SELECT 1'
);
PREPARE stmt_role FROM @sql_add_role;
EXECUTE stmt_role;
DEALLOCATE PREPARE stmt_role;

SET @has_prereq := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND column_name = 'prerequisites'
);
SET @sql_add_prereq := IF(@has_prereq = 0,
    'ALTER TABLE `edu_course` ADD COLUMN `prerequisites` varchar(255) DEFAULT NULL COMMENT ''前置依赖概念或课程'' AFTER `target_role`',
    'SELECT 1'
);
PREPARE stmt_prereq FROM @sql_add_prereq;
EXECUTE stmt_prereq;
DEALLOCATE PREPARE stmt_prereq;

-- 2. 为既有 20 门核心 IT 课程更新标准技能与难度标签（基于 MOOCCubeX / Coursera 体系标定）
UPDATE `edu_course` SET `difficulty_level` = 1, `skills` = 'Vue3,JavaScript,Pinia,VueRouter,Web前端', `target_role` = '前端开发工程师', `prerequisites` = 'HTML/CSS基础,JavaScript基础' WHERE `id` = 1;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'React18,Hooks,Redux,Next.js,前端工程化', `target_role` = '前端开发工程师', `prerequisites` = 'JavaScript高级,ES6语法' WHERE `id` = 2;
UPDATE `edu_course` SET `difficulty_level` = 3, `skills` = 'TypeScript,泛型编程,装饰器,类型系统,Node.js', `target_role` = '资深前端工程师', `prerequisites` = 'JavaScript基础,Vue或React实战经验' WHERE `id` = 3;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'Java,SpringBoot,微服务,MyBatis,SpringCloud', `target_role` = 'Java后端工程师', `prerequisites` = 'Java核心编程,MySQL基础' WHERE `id` = 4;
UPDATE `edu_course` SET `difficulty_level` = 1, `skills` = 'Python,Django,Flask,Web全栈,RESTful API', `target_role` = 'Python全栈工程师', `prerequisites` = '计算机基础,Python语法基础' WHERE `id` = 5;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'Node.js,Express,MongoDB,WebSocket,Koa', `target_role` = '全栈开发工程师', `prerequisites` = 'JavaScript编程,HTTP网络基础' WHERE `id` = 6;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'Flutter,Dart,移动端开发,iOS,Android,跨平台', `target_role` = '移动端开发工程师', `prerequisites` = '面向对象编程,移动应用基础' WHERE `id` = 7;
UPDATE `edu_course` SET `difficulty_level` = 3, `skills` = 'MySQL,索引优化,SQL调优,分库分表,InnoDB', `target_role` = '数据库专家/后端架构师', `prerequisites` = 'SQL基础,关系数据库原理' WHERE `id` = 8;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'Docker,DockerCompose,K8s,DevOps,容器编排', `target_role` = 'DevOps/运维工程师', `prerequisites` = 'Linux系统常用命令' WHERE `id` = 9;
UPDATE `edu_course` SET `difficulty_level` = 1, `skills` = '机器学习,Python,Scikit-learn,TensorFlow,数据挖掘', `target_role` = 'AI算法工程师', `prerequisites` = 'Python基础,高等数学与统计基础' WHERE `id` = 10;
UPDATE `edu_course` SET `difficulty_level` = 3, `skills` = '深度学习,PyTorch,Transformer,CNN,NLP,大模型', `target_role` = '大模型/AI算法工程师', `prerequisites` = '机器学习基础,Python,线性代数' WHERE `id` = 11;
UPDATE `edu_course` SET `difficulty_level` = 3, `skills` = '前端性能优化,WebVitals,渲染优化,CDN缓存,监控', `target_role` = '资深前端架构师', `prerequisites` = '前端框架项目实战,浏览器原理' WHERE `id` = 12;
UPDATE `edu_course` SET `difficulty_level` = 3, `skills` = 'Go,Goroutine,Channel,高并发,网络编程,微服务', `target_role` = 'Go后端开发工程师', `prerequisites` = 'Go语法基础,操作系统并发知识' WHERE `id` = 13;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'Redis,缓存设计,分布式锁,高并发,哨兵集群', `target_role` = '后端技术专家', `prerequisites` = 'MySQL数据库,Java或后端开发基础' WHERE `id` = 14;
UPDATE `edu_course` SET `difficulty_level` = 1, `skills` = '网络安全,Web安全,渗透测试,OWASP,安全攻防', `target_role` = '网络安全工程师', `prerequisites` = '计算机网络基础,Linux基础' WHERE `id` = 15;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'Next.js,React,SSR服务端渲染,全栈开发,SEO优化', `target_role` = '全栈开发工程师', `prerequisites` = 'React开发基础,Node.js基础' WHERE `id` = 16;
UPDATE `edu_course` SET `difficulty_level` = 3, `skills` = 'Rust,内存安全,所有权,系统编程,底层架构', `target_role` = '系统底层架构师', `prerequisites` = 'C/C++编程基础或深入系统开发' WHERE `id` = 17;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = '数据可视化,ECharts,D3.js,数据大屏,Canvas', `target_role` = '数据前端工程师', `prerequisites` = 'JavaScript基础,Web前端开发' WHERE `id` = 18;
UPDATE `edu_course` SET `difficulty_level` = 2, `skills` = 'Unity,C#,3D游戏开发,游戏物理引擎,着色器', `target_role` = '游戏开发工程师', `prerequisites` = 'C#编程基础,面向对象设计' WHERE `id` = 19;
UPDATE `edu_course` SET `difficulty_level` = 3, `skills` = 'Solidity,以太坊,智能合约,Web3,去中心化应用', `target_role` = '区块链开发工程师', `prerequisites` = '面向对象编程,密码学基础' WHERE `id` = 20;

-- 3. 创建用户多维学习画像表 edu_user_portrait
CREATE TABLE IF NOT EXISTS `edu_user_portrait` (
  `id` bigint NOT NULL COMMENT '主键，雪花 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `intended_role` varchar(100) DEFAULT '全栈开发工程师' COMMENT '目标岗位角色',
  `skill_weights` text DEFAULT NULL COMMENT '技能画像掌握度向量 JSON: {"Java":0.85,"SpringBoot":0.75,...}',
  `preferred_difficulty` tinyint NOT NULL DEFAULT 2 COMMENT '自适应偏好难度：1初级，2中级，3高级',
  `learning_style` varchar(50) DEFAULT 'systematic' COMMENT '学习风格：systematic系统型/practical实战型/fast_paced速成型',
  `completion_rate` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '历史完课率百分比 (0.00~100.00)',
  `study_frequency` varchar(50) DEFAULT 'night' COMMENT '活跃时段：night夜猫子/morning早起型/weekend周末突击',
  `total_study_hours` decimal(8,1) NOT NULL DEFAULT 0.0 COMMENT '累计有效学习小时数',
  `price_sensitivity` varchar(32) DEFAULT 'medium' COMMENT '价格敏感度：low/medium/high',
  `tags` text DEFAULT NULL COMMENT '系统综合画像标签列表 JSON 格式',
  `last_calculated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后计算/刷新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_portrait` (`user_id`, `del_flag`),
  KEY `idx_user_portrait_role` (`intended_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户多维学习画像';

-- 4. 插入或初始化演示学员的画像数据 (student_zhang = 2)
INSERT INTO `edu_user_portrait`
(`id`, `user_id`, `intended_role`, `skill_weights`, `preferred_difficulty`, `learning_style`, `completion_rate`, `study_frequency`, `total_study_hours`, `price_sensitivity`, `tags`, `last_calculated_time`, `create_time`, `update_time`, `del_flag`, `version`)
VALUES
(2001, 2, 'Java后端工程师', '{"Java":88,"SpringBoot":80,"Redis":65,"MySQL":72,"Vue3":45,"Docker":55}', 2, 'systematic', 78.50, 'night', 46.5, 'medium', '["Java技术栈", "高自律学习者", "高并发探索者", "系统进阶期", "夜间专注"]', NOW(), NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE
`intended_role` = VALUES(`intended_role`),
`skill_weights` = VALUES(`skill_weights`),
`preferred_difficulty` = VALUES(`preferred_difficulty`),
`learning_style` = VALUES(`learning_style`),
`completion_rate` = VALUES(`completion_rate`),
`study_frequency` = VALUES(`study_frequency`),
`total_study_hours` = VALUES(`total_study_hours`),
`price_sensitivity` = VALUES(`price_sensitivity`),
`tags` = VALUES(`tags`),
`last_calculated_time` = VALUES(`last_calculated_time`);
