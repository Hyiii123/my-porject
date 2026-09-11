-- =========================================================================
-- V30: 创建全真沉浸式 AI 模拟面试与职涯评测超级子系统核心业务表
-- 包含：面试场次表、问答轮次与深度追问表、算法代码手撕沙箱表、终局多维能力报告表
-- =========================================================================

USE `tj_customer`;

-- 1. 面试场次表
CREATE TABLE IF NOT EXISTS `cs_interview_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '场次主键ID',
  `user_id` BIGINT NOT NULL COMMENT '学员用户ID',
  `user_name` VARCHAR(64) DEFAULT NULL COMMENT '学员姓名/昵称',
  `target_job` VARCHAR(64) NOT NULL COMMENT '目标岗位: Java高级开发/后端架构师/AI全栈工程师等',
  `company_target` VARCHAR(64) DEFAULT '大厂通用' COMMENT '目标企业: 阿里巴巴/字节跳动/腾讯/美团/大厂通用',
  `interviewer_style` VARCHAR(32) NOT NULL DEFAULT 'standard' COMMENT '面试官风格: p7_architect(阿里P7)/bytedance_tech(字节二面)/gentle_hr(资深HRBP)/standard',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-进行中, 2-已完成, 3-已终止',
  `current_turn` INT NOT NULL DEFAULT 1 COMMENT '当前所处轮次序号 (1-10)',
  `total_turns` INT NOT NULL DEFAULT 6 COMMENT '规划总轮次数',
  `score` INT DEFAULT NULL COMMENT '综合得分 (0-100)',
  `duration_seconds` INT NOT NULL DEFAULT 0 COMMENT '面试总耗时(秒)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开启时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模拟面试场次表';

-- 2. 面试问答轮次与深度追问表
CREATE TABLE IF NOT EXISTS `cs_interview_turn` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '问答轮次主键ID',
  `session_id` BIGINT NOT NULL COMMENT '面试场次ID',
  `turn_num` INT NOT NULL COMMENT '轮次序号 (1-10)',
  `dimension` VARCHAR(64) NOT NULL DEFAULT '技术基础' COMMENT '考查维度: Java核心/系统设计/高并发/算法/线上排障/HR胜任力',
  `question` VARCHAR(1000) NOT NULL COMMENT '面试官提问',
  `user_answer` TEXT DEFAULT NULL COMMENT '学员回答内容',
  `depth_level` TINYINT NOT NULL DEFAULT 1 COMMENT '追问深度: 1-概念摸底, 2-底层原理, 3-线上极限排障',
  `matched_knowledge_id` BIGINT DEFAULT NULL COMMENT '命中的 10,000 题库 ID',
  `ai_feedback` TEXT DEFAULT NULL COMMENT 'AI面试官点评: 亮点与漏洞分析',
  `standard_reference` TEXT DEFAULT NULL COMMENT '大厂标杆满分答题模板',
  `turn_score` INT DEFAULT NULL COMMENT '本轮得分 (0-100)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `answer_time` DATETIME DEFAULT NULL COMMENT '学员回答时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_turn` (`session_id`, `turn_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试问答轮次与追问表';

-- 3. 算法代码手撕与沙箱评测表
CREATE TABLE IF NOT EXISTS `cs_interview_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '代码提交ID',
  `session_id` BIGINT NOT NULL COMMENT '面试场次ID',
  `turn_id` BIGINT DEFAULT NULL COMMENT '关联的问答轮次ID',
  `problem_title` VARCHAR(255) NOT NULL COMMENT '手撕题目名称',
  `language` VARCHAR(32) NOT NULL DEFAULT 'java' COMMENT '编程语言: java/python/cpp/go',
  `user_code` TEXT NOT NULL COMMENT '学员编写的代码',
  `execution_status` VARCHAR(32) NOT NULL DEFAULT 'accepted' COMMENT '评测状态: accepted/syntax_error/timeout/runtime_error',
  `time_complexity` VARCHAR(64) DEFAULT 'O(n)' COMMENT '推演时间复杂度',
  `space_complexity` VARCHAR(64) DEFAULT 'O(1)' COMMENT '推演空间复杂度',
  `code_smells` TEXT DEFAULT NULL COMMENT '代码异味与设计缺陷审计',
  `refactored_code` TEXT DEFAULT NULL COMMENT 'AI 重构高阶范例代码',
  `passed_test_cases` INT NOT NULL DEFAULT 0 COMMENT '通过用例数',
  `total_test_cases` INT NOT NULL DEFAULT 0 COMMENT '总测试用例数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_code` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试代码手撕与沙箱评测表';

-- 4. 终局多维能力诊断大屏报告表
CREATE TABLE IF NOT EXISTS `cs_interview_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报告主键ID',
  `session_id` BIGINT NOT NULL COMMENT '面试场次ID',
  `offer_decision` VARCHAR(32) NOT NULL DEFAULT 'Hire' COMMENT '终局Offer裁决: Strong Hire / Hire / Weak Hire / Reject',
  `level_match` VARCHAR(64) NOT NULL DEFAULT '对标阿里P6+' COMMENT '大厂职级对标: 阿里P6+/字节2-1/腾讯9级/美团L7等',
  `radar_data` JSON NOT NULL COMMENT '六维能力得分: {"core":88,"architecture":75,"storage":85,"distributed":70,"coding":80,"communication":85}',
  `overall_summary` TEXT NOT NULL COMMENT '面试官委员会综合评语',
  `core_strengths` TEXT DEFAULT NULL COMMENT '三大核心竞争优势与闪光点',
  `critical_weaknesses` TEXT DEFAULT NULL COMMENT '致命短板与失分点透视',
  `speech_refactoring` TEXT DEFAULT NULL COMMENT '答题话术重塑对比 (结巴/罗嗦回答 ➔ 大厂STAR标准范式)',
  `recommended_courses` JSON DEFAULT NULL COMMENT '定向推荐补强的平台课程章节列表',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报告生成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试终局多维能力诊断大屏报告表';
