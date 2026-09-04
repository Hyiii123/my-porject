-- 文件服务媒资元数据表。生产环境由迁移工具执行；初始化数据由 tianji-file.sql 提供。
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS `tj_file` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `tj_file`;

CREATE TABLE IF NOT EXISTS `file_media` (
  `id` bigint NOT NULL,
  `media_name` varchar(255) NOT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_id` varchar(255) DEFAULT NULL,
  `file_url` varchar(1000) DEFAULT NULL,
  `format` varchar(32) NOT NULL DEFAULT 'MP4',
  `size_bytes` bigint NOT NULL DEFAULT 0,
  `duration_seconds` int NOT NULL DEFAULT 0,
  `resolution` varchar(32) DEFAULT NULL,
  `media_type` varchar(32) NOT NULL DEFAULT 'other',
  `status` varchar(32) NOT NULL DEFAULT 'unused',
  `description` varchar(1000) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_media_file_id` (`file_id`),
  KEY `idx_file_media_type_status` (`media_type`, `status`, `create_time`),
  KEY `idx_file_media_name` (`media_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒资元数据';
