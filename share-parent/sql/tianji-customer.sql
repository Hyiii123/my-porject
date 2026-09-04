-- 天机学堂：AI 客服数据库（MySQL 8.x）
-- 数据边界：share-customer
-- AI 服务仅使用第三方 Pixel API，不配置官方 OpenAI API。
-- 默认第三方地址：https://api.ai-pixel.online
-- API Key 不在此脚本中写入；应通过环境变量/Nacos 密钥或后台加密配置注入。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `tj_customer`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `tj_customer`;

CREATE TABLE IF NOT EXISTS `cs_knowledge` (
  `id` bigint NOT NULL,
  `question` varchar(500) NOT NULL,
  `answer` text NOT NULL,
  `keywords` varchar(1000) DEFAULT NULL,
  `category` varchar(64) NOT NULL DEFAULT '其他',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0停用，1启用',
  `hit_count` int NOT NULL DEFAULT 0,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_legacy_id` (`legacy_id`),
  KEY `idx_knowledge_status_category` (`status`, `category`),
  KEY `idx_knowledge_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服知识库';

CREATE TABLE IF NOT EXISTS `cs_faq` (
  `id` bigint NOT NULL,
  `question` varchar(500) NOT NULL,
  `answer` text NOT NULL,
  `category` varchar(64) NOT NULL DEFAULT '其他',
  `sort_num` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `hit_count` int NOT NULL DEFAULT 0,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_faq_legacy_id` (`legacy_id`),
  KEY `idx_faq_enabled_sort` (`enabled`, `sort_num`),
  KEY `idx_faq_category` (`category`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服常见问题';

CREATE TABLE IF NOT EXISTS `cs_session` (
  `id` bigint NOT NULL,
  `session_no` varchar(64) NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '逻辑关联 share-user，访客为空',
  `user_name` varchar(128) NOT NULL DEFAULT '访客用户',
  `source` varchar(32) NOT NULL DEFAULT 'AI客服' COMMENT 'AI客服来源',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0进行中，3已关闭',
  `last_message` varchar(1000) DEFAULT NULL,
  `satisfaction_score` tinyint DEFAULT NULL COMMENT '1-5 分',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `closed_at` datetime DEFAULT NULL,
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  UNIQUE KEY `uk_session_legacy_id` (`legacy_id`),
  KEY `idx_session_user_time` (`user_id`, `updated_at`),
  KEY `idx_session_status_time` (`status`, `updated_at`),
  KEY `idx_session_satisfaction` (`satisfaction_score`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话';

CREATE TABLE IF NOT EXISTS `cs_message` (
  `id` bigint NOT NULL,
  `session_id` bigint NOT NULL,
  `message_type` tinyint NOT NULL COMMENT '1用户，2AI，4系统',
  `sender_id` bigint DEFAULT NULL,
  `sender_name` varchar(128) NOT NULL,
  `content` text NOT NULL,
  `ai_model` varchar(128) DEFAULT NULL,
  `token_usage` int DEFAULT NULL,
  `is_fallback` tinyint NOT NULL DEFAULT 0 COMMENT '是否为本地知识库降级回复',
  `legacy_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_legacy_id` (`legacy_id`),
  KEY `idx_message_session_time` (`session_id`, `create_time`),
  KEY `idx_message_type_time` (`message_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服消息';

CREATE TABLE IF NOT EXISTS `cs_evaluation` (
  `id` bigint NOT NULL,
  `session_id` bigint NOT NULL,
  `score` tinyint NOT NULL COMMENT '1-5 分',
  `tags_json` json DEFAULT NULL,
  `comment` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluation_session` (`session_id`),
  KEY `idx_evaluation_score_time` (`score`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服评价';

CREATE TABLE IF NOT EXISTS `cs_ai_config` (
  `id` bigint NOT NULL,
  `provider` varchar(32) NOT NULL DEFAULT 'pixel' COMMENT '固定为第三方 Pixel 服务',
  `base_url` varchar(500) NOT NULL DEFAULT 'https://api.ai-pixel.online',
  `endpoint_path` varchar(200) NOT NULL DEFAULT '/v1/responses',
  `model` varchar(128) NOT NULL DEFAULT 'gpt-5.5',
  `api_key_ciphertext` text COMMENT '加密后的第三方 Key，禁止明文',
  `enabled` tinyint NOT NULL DEFAULT 0,
  `timeout_ms` int NOT NULL DEFAULT 30000,
  `max_retries` tinyint NOT NULL DEFAULT 1,
  `system_prompt` text,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_config_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方 AI 配置';

CREATE TABLE IF NOT EXISTS `cs_ai_call_log` (
  `id` bigint NOT NULL,
  `request_no` varchar(64) NOT NULL,
  `session_id` bigint DEFAULT NULL,
  `provider` varchar(32) NOT NULL DEFAULT 'pixel',
  `model` varchar(128) DEFAULT NULL,
  `input_tokens` int DEFAULT NULL,
  `output_tokens` int DEFAULT NULL,
  `latency_ms` int DEFAULT NULL,
  `result_status` tinyint NOT NULL DEFAULT 0 COMMENT '0处理中，1成功，2失败，3降级',
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_call_request_no` (`request_no`),
  KEY `idx_ai_call_session_time` (`session_id`, `create_time`),
  KEY `idx_ai_call_status_time` (`result_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用日志';

CREATE TABLE IF NOT EXISTS `cs_operation_log` (
  `id` bigint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(128) DEFAULT NULL,
  `operation_type` varchar(64) NOT NULL,
  `business_type` varchar(64) NOT NULL,
  `business_id` varchar(64) DEFAULT NULL,
  `request_ip` varchar(64) DEFAULT NULL,
  `detail_json` json DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_business_time` (`business_type`, `business_id`, `create_time`),
  KEY `idx_operation_operator_time` (`operator_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服业务操作日志';

-- 当前前端 customerService.js 中的知识库初始化数据。
INSERT INTO `cs_knowledge`
(`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES
(1, '如何注册账号？', '点击页面右上角“注册”，填写手机号和验证码后即可完成注册。', '注册,账号,手机号', '账号与登录', 1, 'kb-1', '2026-08-18 09:00:00'),
(2, '课程支持退款吗？', '课程购买后 7 天内，且学习进度不超过 30% 时可以申请退款。', '退款,退课,售后', '订单售后', 1, 'kb-2', '2026-08-18 09:00:00'),
(3, '有哪些支付方式？', '目前支持微信支付、支付宝和银行卡支付。', '支付,付款,支付宝,微信', '订单支付', 1, 'kb-3', '2026-08-18 09:00:00'),
(4, '如何查看我的学习进度？', '登录后进入“我的课程”，课程卡片会展示最近学习进度，学习页面也会自动保存播放记录。', '学习进度,课程,学习记录', '课程学习', 1, 'kb-4', '2026-08-18 09:00:00'),
(5, '视频播放不了怎么办？', '请先检查网络并刷新页面，建议使用最新版 Chrome、Edge 或 Safari 浏览器；仍无法播放时，请补充设备、浏览器、网络和错误提示，便于进一步排查。', '视频,播放,卡顿,黑屏', '课程学习', 1, 'kb-5', '2026-08-18 09:00:00'),
(6, '课程可以开发票吗？', '可以。完成支付后进入订单详情，点击“申请发票”填写抬头和邮箱，电子发票会在审核后发送。', '发票,抬头,报销,订单', '订单售后', 1, 'kb-6', '2026-08-18 09:00:00'),
(7, '学完课程可以获得证书吗？', '完成课程要求并通过结业考试后，可以在个人中心申请电子结业证书。', '证书,结业,考试', '课程学习', 1, 'kb-7', '2026-08-18 09:00:00'),
(8, '忘记密码怎么处理？', '可以在登录页使用短信登录，登录后到个人设置中重新设置密码。', '密码,忘记密码,登录', '账号与登录', 1, 'kb-8', '2026-08-18 09:00:00'),
(9, '优惠券在哪里使用？', '在课程结算页选择可用优惠券即可抵扣；每笔订单通常只能使用一张优惠券。', '优惠券,抵扣,优惠', '优惠活动', 1, 'kb-9', '2026-08-18 09:00:00'),
(10, '客服中心可以咨询哪些内容？', '客服中心支持解答账号登录、课程学习、订单支付、发票和优惠活动等常见问题。', '客服,咨询,帮助,账号,课程,订单', '客服服务', 1, 'kb-10', '2026-08-18 09:00:00')
ON DUPLICATE KEY UPDATE
`question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`),
`category` = VALUES(`category`), `status` = VALUES(`status`);

INSERT INTO `cs_faq`
(`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES
(1, '如何领取新用户优惠券？', '登录后打开首页优惠券入口即可领取新用户专享券。', '优惠活动', 1, 1, 'faq-1', '2026-08-18 09:00:00'),
(2, '购买课程后在哪里学习？', '进入“我的课程”即可查看已购买课程和学习计划。', '课程学习', 2, 1, 'faq-2', '2026-08-18 09:00:00'),
(3, '客服中心可以咨询哪些内容？', '客服中心支持解答账号登录、课程学习、订单支付、发票和优惠活动等常见问题。', '客服服务', 3, 1, 'faq-3', '2026-08-18 09:00:00'),
(4, '忘记密码怎么办？', '在登录页选择短信登录，登录后进入个人设置即可重置密码。', '账号与登录', 4, 1, 'faq-4', '2026-08-18 09:00:00'),
(5, '视频播放卡顿怎么解决？', '建议切换网络并刷新页面，使用 Chrome 或 Edge 浏览器体验更佳。', '课程学习', 5, 1, 'faq-5', '2026-08-18 09:00:00'),
(6, '课程是否支持开发票？', '支持电子发票，进入订单详情即可申请。', '订单售后', 6, 1, 'faq-6', '2026-08-18 09:00:00'),
(7, '优惠券可以叠加使用吗？', '每笔订单默认只能使用一张优惠券，具体以结算页展示为准。', '优惠活动', 7, 1, 'faq-7', '2026-08-18 09:00:00'),
(8, '课程购买后有效期多久？', '已购买课程通常支持长期学习，具体有效期以课程详情页说明为准。', '课程学习', 8, 1, 'faq-8', '2026-08-18 09:00:00'),
(9, '如何修改个人资料？', '登录后进入个人中心-设置，即可修改头像、昵称和联系方式。', '账号与登录', 9, 1, 'faq-9', '2026-08-18 09:00:00'),
(10, '订单支付失败怎么办？', '请检查支付账户状态、余额和支付限额后重新发起支付；如果仍然失败，请补充订单号、支付时间和错误提示。', '订单支付', 10, 1, 'faq-10', '2026-08-18 09:00:00')
ON DUPLICATE KEY UPDATE
`question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`),
`sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`);

-- 默认关闭，必须配置第三方 Key 后由管理员启用；这里不写入任何 Key。
INSERT INTO `cs_ai_config`
(`id`, `provider`, `base_url`, `endpoint_path`, `model`, `enabled`, `timeout_ms`, `max_retries`)
VALUES (1, 'pixel', 'https://api.ai-pixel.online', '/v1/responses', 'gpt-5.5', 0, 30000, 1)
ON DUPLICATE KEY UPDATE
`base_url` = VALUES(`base_url`), `endpoint_path` = VALUES(`endpoint_path`),
`model` = VALUES(`model`), `timeout_ms` = VALUES(`timeout_ms`), `max_retries` = VALUES(`max_retries`);

-- 校验示例：
-- SELECT COUNT(*) AS knowledge_count FROM cs_knowledge WHERE del_flag = 0;
-- SELECT COUNT(*) AS faq_count FROM cs_faq WHERE del_flag = 0;
-- 当前静态数据预期为知识库 10 条、常见问题 10 条。
