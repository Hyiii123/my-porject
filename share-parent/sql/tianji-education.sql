-- 天机学堂：教育业务数据库（MySQL 8.x）
--
-- 数据边界：share-education
-- 说明：本脚本不创建跨服务外键。user_id、course_id 等字段通过服务接口关联，
--       这样可以保持微服务的数据边界；legacy_id 用于静态 Mock 数据的幂等迁移。
-- 金额说明：课程价格按元保存，原始 JSON 中的 price/originalPrice 为分，迁移时除以 100。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `tj_education`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `tj_education`;

-- 课程分类
CREATE TABLE IF NOT EXISTS `edu_category` (
  `id` bigint NOT NULL COMMENT '主键，雪花 ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类 ID，0 表示根分类',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `description` varchar(500) DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(200) DEFAULT NULL COMMENT '图标标识或地址',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  `legacy_id` varchar(64) DEFAULT NULL COMMENT '原 Mock 数据 ID',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_legacy_id` (`legacy_id`),
  KEY `idx_category_parent_status` (`parent_id`, `status`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类';

-- 教师扩展资料；user_id 指向 share-user 的用户 ID
CREATE TABLE IF NOT EXISTS `edu_teacher` (
  `id` bigint NOT NULL COMMENT '主键，雪花 ID',
  `user_id` bigint DEFAULT NULL COMMENT '关联用户服务 ID',
  `teacher_name` varchar(100) NOT NULL COMMENT '教师姓名',
  `avatar_url` varchar(500) DEFAULT NULL,
  `title` varchar(150) DEFAULT NULL COMMENT '职称或身份',
  `introduction` text COMMENT '教师简介',
  `specialty` varchar(500) DEFAULT NULL COMMENT '擅长领域',
  `course_count` int NOT NULL DEFAULT 0 COMMENT '展示用课程数快照',
  `student_count` int NOT NULL DEFAULT 0 COMMENT '展示用学员数快照',
  `rating` decimal(3,2) NOT NULL DEFAULT 0.00,
  `status` tinyint NOT NULL DEFAULT 1,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_legacy_id` (`legacy_id`),
  KEY `idx_teacher_user` (`user_id`),
  KEY `idx_teacher_status_rating` (`status`, `rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师资料';

-- 课程主表
CREATE TABLE IF NOT EXISTS `edu_course` (
  `id` bigint NOT NULL COMMENT '主键，雪花 ID',
  `category_id` bigint NOT NULL COMMENT '课程分类 ID',
  `course_code` varchar(64) DEFAULT NULL COMMENT '课程业务编码',
  `course_name` varchar(200) NOT NULL COMMENT '课程名称',
  `cover_url` varchar(500) DEFAULT NULL,
  `short_description` varchar(500) DEFAULT NULL,
  `description` text,
  `price` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '售价，单位元',
  `original_price` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '原价，单位元',
  `lesson_count` int NOT NULL DEFAULT 0,
  `learner_count` int NOT NULL DEFAULT 0 COMMENT '展示用学习人数',
  `duration_minutes` int NOT NULL DEFAULT 0,
  `rating` decimal(3,2) NOT NULL DEFAULT 0.00,
  `is_free` tinyint NOT NULL DEFAULT 0,
  `is_recommended` tinyint NOT NULL DEFAULT 0,
  `is_hot` tinyint NOT NULL DEFAULT 0,
  `is_new` tinyint NOT NULL DEFAULT 0,
  `sort_num` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0草稿，1上架，2下架',
  `publish_time` datetime DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  UNIQUE KEY `uk_course_legacy_id` (`legacy_id`),
  KEY `idx_course_category_status` (`category_id`, `status`, `sort_num`),
  KEY `idx_course_recommend` (`is_recommended`, `status`, `sort_num`),
  KEY `idx_course_name` (`course_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程';

CREATE TABLE IF NOT EXISTS `edu_course_teacher` (
  `id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `teacher_role` varchar(32) NOT NULL DEFAULT '讲师',
  `sort_num` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_teacher` (`course_id`, `teacher_id`),
  KEY `idx_course_teacher_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程教师关联';

-- 课程目录。media_id 指向 share-file 的媒资 ID
CREATE TABLE IF NOT EXISTS `edu_course_catalog` (
  `id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `catalog_title` varchar(200) NOT NULL,
  `catalog_type` tinyint NOT NULL DEFAULT 1 COMMENT '1章节，2课时',
  `media_id` bigint DEFAULT NULL,
  `media_name` varchar(255) DEFAULT NULL COMMENT '媒资名称快照',
  `duration_seconds` int NOT NULL DEFAULT 0,
  `is_free` tinyint NOT NULL DEFAULT 0,
  `trailer` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许试看',
  `sort_num` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_legacy_id` (`legacy_id`),
  KEY `idx_catalog_course_parent` (`course_id`, `parent_id`, `sort_num`),
  KEY `idx_catalog_media` (`media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程目录';

-- 课程目录小节与题库题目的关联，不跨服务建立外键。
CREATE TABLE IF NOT EXISTS `edu_catalog_question` (
  `id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `catalog_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `sort_num` int NOT NULL DEFAULT 0,
  `score` decimal(8,2) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_question` (`catalog_id`, `question_id`),
  KEY `idx_catalog_question_course` (`course_id`, `catalog_id`, `sort_num`),
  KEY `idx_catalog_question_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程小节题目关联';

CREATE TABLE IF NOT EXISTS `edu_banner` (
  `id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `target_type` varchar(32) DEFAULT NULL COMMENT 'course/url/none',
  `target_value` varchar(500) DEFAULT NULL,
  `sort_num` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_banner_legacy_id` (`legacy_id`),
  KEY `idx_banner_status_time` (`status`, `sort_num`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页 Banner';

CREATE TABLE IF NOT EXISTS `edu_course_recommend` (
  `id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `recommend_type` varchar(32) NOT NULL COMMENT 'home/hot/new/interest 等',
  `sort_num` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_recommend_type` (`course_id`, `recommend_type`),
  KEY `idx_recommend_type_sort` (`recommend_type`, `status`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程推荐关系';

CREATE TABLE IF NOT EXISTS `edu_interest` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interest_user_category` (`user_id`, `category_id`),
  KEY `idx_interest_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣';

CREATE TABLE IF NOT EXISTS `edu_learning_plan` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `plan_name` varchar(200) DEFAULT NULL,
  `target_date` date DEFAULT NULL,
  `daily_minutes` int NOT NULL DEFAULT 30,
  `progress_percent` decimal(5,2) NOT NULL DEFAULT 0.00,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1进行中，2完成，3暂停',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learning_plan_user_course` (`user_id`, `course_id`, `del_flag`),
  KEY `idx_learning_plan_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划';

CREATE TABLE IF NOT EXISTS `edu_learning_record` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `catalog_id` bigint DEFAULT NULL,
  `progress_percent` decimal(5,2) NOT NULL DEFAULT 0.00,
  `progress_seconds` int NOT NULL DEFAULT 0,
  `learn_duration_seconds` int NOT NULL DEFAULT 0,
  `completed_lessons` int NOT NULL DEFAULT 0,
  `total_lessons` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 3 COMMENT '1学习中，2已完成，3未开始',
  `last_learn_time` datetime DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learning_legacy_id` (`legacy_id`),
  UNIQUE KEY `uk_learning_user_course_catalog` (`user_id`, `course_id`, `catalog_id`, `del_flag`),
  KEY `idx_learning_user_time` (`user_id`, `last_learn_time`),
  KEY `idx_learning_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录';

-- 社区问答
CREATE TABLE IF NOT EXISTS `edu_question` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `title` varchar(300) NOT NULL,
  `content` text NOT NULL,
  `category` varchar(64) DEFAULT NULL,
  `view_count` int NOT NULL DEFAULT 0,
  `reply_count` int NOT NULL DEFAULT 0,
  `like_count` int NOT NULL DEFAULT 0,
  `hidden` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_question_course_time` (`course_id`, `create_time`),
  KEY `idx_question_user_time` (`user_id`, `create_time`),
  KEY `idx_question_status_time` (`hidden`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户问答';

CREATE TABLE IF NOT EXISTS `edu_reply` (
  `id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `like_count` int NOT NULL DEFAULT 0,
  `hidden` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_reply_question_time` (`question_id`, `create_time`),
  KEY `idx_reply_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答回复';

CREATE TABLE IF NOT EXISTS `edu_question_like` (
  `id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_like` (`question_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题点赞';

-- 用户笔记及互动
CREATE TABLE IF NOT EXISTS `edu_note` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `catalog_id` bigint DEFAULT NULL,
  `title` varchar(300) NOT NULL,
  `content` text NOT NULL,
  `visibility` tinyint NOT NULL DEFAULT 1 COMMENT '1公开，0私有',
  `like_count` int NOT NULL DEFAULT 0,
  `collect_count` int NOT NULL DEFAULT 0,
  `hidden` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_note_user_time` (`user_id`, `create_time`),
  KEY `idx_note_course_time` (`course_id`, `create_time`),
  KEY `idx_note_public_status` (`visibility`, `hidden`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习笔记';

CREATE TABLE IF NOT EXISTS `edu_note_collect` (
  `id` bigint NOT NULL,
  `note_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_collect` (`note_id`, `user_id`),
  KEY `idx_note_collect_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记收藏';

CREATE TABLE IF NOT EXISTS `edu_note_like` (
  `id` bigint NOT NULL,
  `note_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_like` (`note_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记点赞';

-- 考试与题库
CREATE TABLE IF NOT EXISTS `edu_exam` (
  `id` bigint NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `exam_name` varchar(200) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `total_score` decimal(8,2) NOT NULL DEFAULT 100.00,
  `pass_score` decimal(8,2) NOT NULL DEFAULT 60.00,
  `duration_minutes` int NOT NULL DEFAULT 60,
  `status` tinyint NOT NULL DEFAULT 1,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_legacy_id` (`legacy_id`),
  KEY `idx_exam_course_status` (`course_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试';

CREATE TABLE IF NOT EXISTS `edu_exam_question_bank` (
  `id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL COMMENT '课程分类 ID，兼容旧题库分类筛选',
  `question_type` varchar(32) NOT NULL COMMENT 'single/multiple/judge/blank',
  `stem` text NOT NULL,
  `options_json` json DEFAULT NULL,
  `correct_answer` varchar(1000) DEFAULT NULL,
  `analysis` text,
  `score` decimal(8,2) NOT NULL DEFAULT 0.00,
  `difficulty` tinyint NOT NULL DEFAULT 2,
  `status` tinyint NOT NULL DEFAULT 1,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bank_category` (`category_id`),
  UNIQUE KEY `uk_bank_question_legacy_id` (`legacy_id`),
  KEY `idx_bank_type_status` (`question_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题库';

CREATE TABLE IF NOT EXISTS `edu_exam_question` (
  `id` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `sort_num` int NOT NULL DEFAULT 0,
  `score` decimal(8,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_question` (`exam_id`, `question_id`),
  KEY `idx_exam_question_sort` (`exam_id`, `sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题目关联';

CREATE TABLE IF NOT EXISTS `edu_exam_record` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `score` decimal(8,2) DEFAULT NULL,
  `total_score` decimal(8,2) NOT NULL DEFAULT 100.00,
  `correct_count` int NOT NULL DEFAULT 0,
  `question_count` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0进行中，1通过，2未通过',
  `started_at` datetime DEFAULT NULL,
  `submitted_at` datetime DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_record_legacy_id` (`legacy_id`),
  KEY `idx_exam_record_user_time` (`user_id`, `create_time`),
  KEY `idx_exam_record_exam` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录';

CREATE TABLE IF NOT EXISTS `edu_exam_answer` (
  `id` bigint NOT NULL,
  `record_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `user_answer` varchar(1000) DEFAULT NULL,
  `is_correct` tinyint NOT NULL DEFAULT 0,
  `score` decimal(8,2) NOT NULL DEFAULT 0.00,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_answer` (`record_id`, `question_id`),
  KEY `idx_exam_answer_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试答题记录';

-- 积分与签到
CREATE TABLE IF NOT EXISTS `edu_sign_record` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `sign_date` date NOT NULL,
  `points` int NOT NULL DEFAULT 0,
  `continuous_days` int NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sign_user_date` (`user_id`, `sign_date`),
  KEY `idx_sign_date` (`sign_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到';

CREATE TABLE IF NOT EXISTS `edu_points_ledger` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `change_amount` int NOT NULL COMMENT '本次变动，可正可负',
  `balance_after` int NOT NULL DEFAULT 0,
  `source_type` varchar(32) NOT NULL COMMENT 'sign/exam/course/manual 等',
  `biz_id` varchar(64) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_points_biz` (`user_id`, `source_type`, `biz_id`),
  KEY `idx_points_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- 工作台日统计。指标由访问、订单和学员事件按日汇总，页面只消费接口结果。
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

-- 以下为当前前端静态数据中的分类、教师和课程初始化数据。
-- source_legacy_id 保留原始 ID，后续迁移程序可安全重复执行。
INSERT INTO `edu_category`
(`id`, `category_name`, `description`, `icon`, `sort_num`, `status`, `legacy_id`, `create_time`)
VALUES
(1, '前端开发', '学习Web前端技术，包括HTML、CSS、JavaScript等', 'frontend', 1, 1, '1', '2024-01-01 00:00:00'),
(2, '后端开发', '学习后端开发技术，包括Java、Python、Node.js等', 'backend', 2, 1, '2', '2024-01-01 00:00:00'),
(3, '移动开发', '学习移动端开发技术，包括Android、iOS、Flutter等', 'mobile', 3, 1, '3', '2024-01-01 00:00:00'),
(4, '数据库', '学习数据库技术，包括MySQL、MongoDB、Redis等', 'database', 4, 1, '4', '2024-01-01 00:00:00'),
(5, '云计算与DevOps', '学习云计算和DevOps技术，包括Docker、K8s、AWS等', 'cloud', 5, 1, '5', '2024-01-01 00:00:00'),
(6, '人工智能', '学习AI技术，包括机器学习、深度学习、NLP等', 'ai', 6, 1, '6', '2024-01-01 00:00:00'),
(7, '数据科学', '学习数据分析和可视化技术', 'data-science', 7, 1, '7', '2024-01-01 00:00:00'),
(8, '网络安全', '学习网络安全和渗透测试技术', 'security', 8, 1, '8', '2024-01-01 00:00:00'),
(9, '游戏开发', '学习游戏开发技术，包括Unity、Unreal等', 'game', 9, 1, '9', '2024-01-01 00:00:00'),
(10, '区块链', '学习区块链和Web3开发技术', 'blockchain', 10, 1, '10', '2024-01-01 00:00:00')
ON DUPLICATE KEY UPDATE
`category_name` = VALUES(`category_name`), `description` = VALUES(`description`),
`icon` = VALUES(`icon`), `sort_num` = VALUES(`sort_num`), `status` = VALUES(`status`);

INSERT INTO `edu_teacher`
(`id`, `teacher_name`, `avatar_url`, `title`, `introduction`, `specialty`, `course_count`, `student_count`, `rating`, `status`, `legacy_id`)
VALUES
(1, '张老师', '/src/assets/images/teachers/teacher1.svg', '高级前端工程师', '10年前端开发经验，曾就职于BAT，精通Vue、React、Angular等主流框架', 'Vue3、React、TypeScript', 5, 25680, 4.90, 1, '1'),
(2, '李老师', '/src/assets/images/teachers/teacher2.svg', '全栈技术专家', '8年全栈开发经验，精通React、Node.js、Next.js等技术栈', 'React、Node.js、Next.js', 4, 18950, 4.80, 1, '2'),
(3, '王老师', '/src/assets/images/teachers/teacher3.svg', 'Java架构师', '12年Java开发经验，微服务架构专家，精通SpringCloud生态', 'Java、SpringBoot、微服务', 6, 32560, 4.90, 1, '3'),
(4, '赵老师', '/src/assets/images/teachers/teacher4.svg', 'Python高级工程师', '9年Python开发经验，AI和数据分析领域专家', 'Python、Django、Flask', 5, 28920, 4.80, 1, '4'),
(5, '陈老师', '/src/assets/images/teachers/teacher5.svg', '移动端开发专家', '7年移动端开发经验，精通Flutter、React Native、原生开发', 'Flutter、React Native、iOS', 3, 12780, 4.70, 1, '5'),
(6, '刘老师', '/src/assets/images/teachers/teacher6.svg', '数据库专家', '11年数据库架构经验，精通MySQL、MongoDB、Redis等', 'MySQL、Redis、MongoDB', 4, 20150, 4.80, 1, '6'),
(7, '杨老师', '/src/assets/images/teachers/teacher7.svg', 'DevOps工程师', '6年运维开发经验，云原生和容器化技术专家', 'Docker、K8s、AWS', 3, 15870, 4.70, 1, '7'),
(8, '黄老师', '/src/assets/images/teachers/teacher8.svg', 'AI算法工程师', '8年AI研发经验，深度学习和NLP领域专家', '机器学习、深度学习、NLP', 4, 23480, 4.90, 1, '8'),
(9, '周老师', '/src/assets/images/teachers/teacher9.svg', '系统架构师', '10年系统编程经验，精通Go、Rust等高性能语言', 'Go、Rust、系统编程', 3, 11770, 4.80, 1, '9'),
(10, '吴老师', '/src/assets/images/teachers/teacher10.svg', '安全工程师', '7年网络安全经验，渗透测试和安全防护专家', 'Web安全、渗透测试', 2, 9540, 4.70, 1, '10'),
(11, '郑老师', '/src/assets/images/teachers/teacher11.svg', '数据分析师', '6年数据分析经验，数据可视化和BI专家', 'ECharts、D3.js、数据分析', 3, 14890, 4.80, 1, '11'),
(12, '孙老师', '/src/assets/images/teachers/teacher12.svg', '游戏开发工程师', '8年游戏开发经验，Unity和Unreal引擎专家', 'Unity、C#、游戏设计', 2, 8670, 4.70, 1, '12'),
(13, '马老师', '/src/assets/images/teachers/teacher13.svg', '区块链开发工程师', '5年区块链开发经验，Web3和智能合约专家', 'Solidity、Web3、DeFi', 2, 6450, 4.60, 1, '13')
ON DUPLICATE KEY UPDATE
`teacher_name` = VALUES(`teacher_name`), `avatar_url` = VALUES(`avatar_url`),
`title` = VALUES(`title`), `introduction` = VALUES(`introduction`),
`specialty` = VALUES(`specialty`), `course_count` = VALUES(`course_count`),
`student_count` = VALUES(`student_count`), `rating` = VALUES(`rating`);

INSERT INTO `edu_course`
(`id`, `category_id`, `course_name`, `cover_url`, `short_description`, `price`, `original_price`, `lesson_count`, `learner_count`, `is_free`, `is_recommended`, `is_hot`, `is_new`, `sort_num`, `status`, `legacy_id`, `create_time`)
VALUES
(1, 1, 'Vue3 从入门到精通', '/src/assets/images/courses/vue3.svg', '全面掌握Vue3核心语法、组合式API、Pinia状态管理、Vue Router等', 199.00, 399.00, 48, 12580, 0, 1, 1, 0, 1, 1, '1', '2024-01-15 10:00:00'),
(2, 1, 'React18 实战教程', '/src/assets/images/courses/react.svg', '深入学习React18新特性、Hooks、Redux、Next.js等', 249.00, 499.00, 60, 9850, 0, 1, 1, 0, 2, 1, '2', '2024-02-01 10:00:00'),
(3, 1, 'TypeScript 高级编程', '/src/assets/images/courses/typescript.svg', '掌握TypeScript高级类型、泛型、装饰器等高级特性', 149.00, 299.00, 36, 7560, 0, 0, 0, 0, 3, 1, '3', '2024-02-15 10:00:00'),
(4, 2, 'Java SpringBoot 实战', '/src/assets/images/courses/springboot.svg', '从零开始学习SpringBoot，掌握微服务架构设计', 299.00, 599.00, 72, 15680, 0, 1, 1, 0, 4, 1, '4', '2024-01-20 10:00:00'),
(5, 2, 'Python 全栈开发', '/src/assets/images/courses/python.svg', 'Python基础、Django/Flask框架、数据库、API开发', 229.00, 459.00, 56, 18920, 0, 0, 1, 0, 5, 1, '5', '2024-02-10 10:00:00'),
(6, 2, 'Node.js 后端开发', '/src/assets/images/courses/nodejs.svg', 'Express/Koa框架、MongoDB、WebSocket、微服务', 189.00, 379.00, 42, 8450, 0, 0, 0, 1, 6, 1, '6', '2024-03-01 10:00:00'),
(7, 3, 'Flutter 跨平台开发', '/src/assets/images/courses/flutter.svg', '使用Flutter开发iOS和Android应用', 269.00, 539.00, 52, 6780, 0, 0, 0, 0, 7, 1, '7', '2024-02-20 10:00:00'),
(8, 4, 'MySQL 数据库优化', '/src/assets/images/courses/mysql.svg', '索引优化、查询优化、分库分表、主从复制', 169.00, 339.00, 38, 11230, 0, 1, 1, 0, 8, 1, '8', '2024-01-25 10:00:00'),
(9, 5, 'Docker 容器化实战', '/src/assets/images/courses/docker.svg', 'Docker基础、Docker Compose、K8s入门', 159.00, 319.00, 32, 9870, 0, 0, 1, 0, 9, 1, '9', '2024-02-05 10:00:00'),
(10, 6, '机器学习入门', '/src/assets/images/courses/ml.svg', 'Python机器学习、Scikit-learn、TensorFlow基础', 349.00, 699.00, 68, 14560, 0, 1, 1, 0, 10, 1, '10', '2024-01-30 10:00:00'),
(11, 6, '深度学习实战', '/src/assets/images/courses/dl.svg', 'PyTorch、CNN、RNN、Transformer架构', 399.00, 799.00, 76, 8920, 0, 1, 0, 0, 11, 1, '11', '2024-02-15 10:00:00'),
(12, 1, 'Web前端性能优化', '/src/assets/images/courses/performance.svg', '加载优化、渲染优化、缓存策略、性能监控', 129.00, 259.00, 28, 5680, 0, 0, 0, 1, 12, 1, '12', '2024-03-10 10:00:00'),
(13, 2, 'Go语言并发编程', '/src/assets/images/courses/golang.svg', 'Goroutine、Channel、并发模式、性能调优', 219.00, 439.00, 44, 7450, 0, 0, 0, 1, 13, 1, '13', '2024-02-28 10:00:00'),
(14, 4, 'Redis 高级应用', '/src/assets/images/courses/redis.svg', '数据结构、持久化、集群、缓存设计模式', 139.00, 279.00, 30, 8920, 0, 0, 1, 1, 14, 1, '14', '2024-03-05 10:00:00'),
(15, 8, '网络安全入门', '/src/assets/images/courses/security.svg', 'Web安全、渗透测试、安全防护', 199.00, 399.00, 40, 6540, 0, 0, 0, 0, 15, 1, '15', '2024-02-12 10:00:00'),
(16, 1, 'Next.js 全栈开发', '/src/assets/images/courses/nextjs.svg', 'SSR、SSG、API Routes、部署优化', 279.00, 559.00, 58, 5680, 0, 0, 1, 1, 16, 1, '16', '2024-03-15 10:00:00'),
(17, 2, 'Rust 系统编程', '/src/assets/images/courses/rust.svg', '所有权、生命周期、并发、系统编程', 329.00, 659.00, 64, 4320, 0, 0, 0, 1, 17, 1, '17', '2024-03-20 10:00:00'),
(18, 7, '数据可视化', '/src/assets/images/courses/visualization.svg', 'ECharts、D3.js、数据大屏设计', 179.00, 359.00, 35, 7890, 0, 0, 0, 0, 18, 1, '18', '2024-02-25 10:00:00'),
(19, 9, 'Unity 游戏开发', '/src/assets/images/courses/unity.svg', 'C#基础、Unity编辑器、2D/3D游戏开发', 299.00, 599.00, 62, 5670, 0, 0, 0, 0, 19, 1, '19', '2024-03-08 10:00:00'),
(20, 10, 'Solidity 智能合约', '/src/assets/images/courses/solidity.svg', 'Solidity语法、智能合约开发、DApp开发', 259.00, 519.00, 48, 3450, 0, 0, 0, 0, 20, 1, '20', '2024-03-12 10:00:00')
ON DUPLICATE KEY UPDATE
`category_id` = VALUES(`category_id`), `course_name` = VALUES(`course_name`),
`cover_url` = VALUES(`cover_url`), `short_description` = VALUES(`short_description`),
`price` = VALUES(`price`), `original_price` = VALUES(`original_price`),
`lesson_count` = VALUES(`lesson_count`), `learner_count` = VALUES(`learner_count`),
`is_recommended` = VALUES(`is_recommended`), `is_hot` = VALUES(`is_hot`),
`is_new` = VALUES(`is_new`), `sort_num` = VALUES(`sort_num`), `status` = VALUES(`status`);

INSERT INTO `edu_course_teacher` (`id`, `course_id`, `teacher_id`, `sort_num`)
VALUES
(10001, 1, 1, 1), (10002, 2, 2, 1), (10003, 3, 1, 1),
(10004, 4, 3, 1), (10005, 5, 4, 1), (10006, 6, 2, 1),
(10007, 7, 5, 1), (10008, 8, 6, 1), (10009, 9, 7, 1),
(10010, 10, 8, 1), (10011, 11, 8, 1), (10012, 12, 1, 1),
(10013, 13, 9, 1), (10014, 14, 6, 1), (10015, 15, 10, 1),
(10016, 16, 2, 1), (10017, 17, 9, 1), (10018, 18, 11, 1),
(10019, 19, 12, 1), (10020, 20, 13, 1)
ON DUPLICATE KEY UPDATE `sort_num` = VALUES(`sort_num`);

-- 工作台演示快照写入数据库，日期相对当前日期生成，避免页面依赖固定日期或硬编码指标。
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

-- 迁移校验示例：
-- SELECT COUNT(*) AS category_count FROM edu_category WHERE del_flag = 0;
-- SELECT COUNT(*) AS course_count FROM edu_course WHERE del_flag = 0;
-- 预期当前静态数据分别为 10 和 20；学习、考试、订单数据在交易脚本或迁移程序中导入。
