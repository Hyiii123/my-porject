-- 客服服务迁移：移除未使用的“转人工”结构。
-- 当前客服仅支持 AI 会话、知识库、常见问题、会话记录和服务评价。
-- 本脚本可重复执行，兼容已经执行过旧版客服建表脚本的环境。
SET NAMES utf8mb4;
USE `tj_customer`;

DROP TABLE IF EXISTS `cs_transfer_record`;

SET @drop_agent_index_sql = (
  SELECT IF(COUNT(*) > 0,
      'ALTER TABLE `cs_session` DROP INDEX `idx_session_agent_status`',
      'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'cs_session'
    AND index_name = 'idx_session_agent_status'
);
PREPARE stmt FROM @drop_agent_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_agent_id_sql = (
  SELECT IF(COUNT(*) > 0,
      'ALTER TABLE `cs_session` DROP COLUMN `assigned_agent_id`',
      'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'cs_session'
    AND column_name = 'assigned_agent_id'
);
PREPARE stmt FROM @drop_agent_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_agent_name_sql = (
  SELECT IF(COUNT(*) > 0,
      'ALTER TABLE `cs_session` DROP COLUMN `assigned_agent_name`',
      'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'cs_session'
    AND column_name = 'assigned_agent_name'
);
PREPARE stmt FROM @drop_agent_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `cs_session`
  MODIFY COLUMN `source` varchar(32) NOT NULL DEFAULT 'AI客服' COMMENT 'AI客服来源',
  MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '0进行中，3已关闭';

ALTER TABLE `cs_message`
  MODIFY COLUMN `message_type` tinyint NOT NULL COMMENT '1用户，2AI，4系统';
