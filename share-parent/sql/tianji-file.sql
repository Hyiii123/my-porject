-- 天机学堂：文件与媒资数据库（MySQL 8.x）
-- 文件二进制默认存储在 share-file 本地卷，file_media 只保存可迁移的元数据。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `tj_file`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `tj_file`;

CREATE TABLE IF NOT EXISTS `file_media` (
  `id` bigint NOT NULL COMMENT '媒资主键，雪花 ID',
  `media_name` varchar(255) NOT NULL COMMENT '媒资展示名称',
  `file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_id` varchar(255) DEFAULT NULL COMMENT '外部媒资 ID，兼容旧腾讯云数据',
  `file_url` varchar(1000) DEFAULT NULL COMMENT '文件访问地址',
  `format` varchar(32) NOT NULL DEFAULT 'MP4',
  `size_bytes` bigint NOT NULL DEFAULT 0,
  `duration_seconds` int NOT NULL DEFAULT 0,
  `resolution` varchar(32) DEFAULT NULL,
  `media_type` varchar(32) NOT NULL DEFAULT 'other' COMMENT 'course/promo/other',
  `status` varchar(32) NOT NULL DEFAULT 'unused' COMMENT 'used/unused/processing',
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
  KEY `idx_file_media_name` (`media_name`),
  KEY `idx_file_media_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒资元数据';

INSERT INTO `file_media`
(`id`,`media_name`,`file_name`,`file_id`,`file_url`,`format`,`size_bytes`,`duration_seconds`,`resolution`,`media_type`,`status`,`description`,`create_time`)
VALUES
(1,'Vue3基础教程-第1章','vue3-basic-01.mp4','legacy-vue3-01','/file/profile/media/vue3-basic-01.mp4','MP4',268435456,2730,'1920x1080','course','used','Vue3基础教程第一章', '2026-05-10 10:00:00'),
(2,'Vue3基础教程-第2章','vue3-basic-02.mp4','legacy-vue3-02','/file/profile/media/vue3-basic-02.mp4','MP4',327155712,3135,'1920x1080','course','used','Vue3基础教程第二章', '2026-05-10 10:05:00'),
(3,'React入门教程-第1章','react-basic-01.mp4','legacy-react-01','/file/profile/media/react-basic-01.mp4','MP4',302989824,2900,'1920x1080','course','used','React入门教程第一章', '2026-05-10 11:00:00'),
(4,'Java SpringBoot实战-第1章','springboot-practice-01.mp4','legacy-springboot-01','/file/profile/media/springboot-practice-01.mp4','MP4',361758720,3525,'1920x1080','course','used','Java SpringBoot实战第一章', '2026-05-10 12:00:00'),
(5,'Python基础教程-第1章','python-basic-01.mp4','legacy-python-01','/file/profile/media/python-basic-01.mp4','MP4',291504128,2790,'1920x1080','course','used','Python基础教程第一章', '2026-05-10 13:00:00'),
(6,'Docker入门教程','docker-basic.mp4','legacy-docker-01','/file/profile/media/docker-basic.mp4','MP4',207618048,2120,'1280x720','course','used','Docker入门教程', '2026-05-10 14:00:00'),
(7,'MySQL优化技巧','mysql-tuning.mp4','legacy-mysql-01','/file/profile/media/mysql-tuning.mp4','MP4',443547648,4335,'1920x1080','course','used','MySQL优化技巧', '2026-05-10 15:00:00'),
(8,'平台宣传视频','platform-promo.mp4','legacy-promo-01','/file/profile/media/platform-promo.mp4','MP4',163577856,150,'1920x1080','promo','used','智问学伴平台宣传视频', '2026-05-10 16:00:00'),
(9,'机器学习入门-第1章','machine-learning-01.mp4','legacy-ml-01','/file/profile/media/machine-learning-01.mp4','MP4',384827392,3765,'1920x1080','course','used','机器学习入门第一章', '2026-05-10 17:00:00'),
(10,'网络安全基础','network-security.mp4','legacy-security-01','/file/profile/media/network-security.mp4','MP4',302989824,2910,'1920x1080','course','used','网络安全基础教程', '2026-05-10 18:00:00'),
(11,'Flutter开发入门','flutter-basic.mp4','legacy-flutter-01','/file/profile/media/flutter-basic.mp4','MP4',327155712,3140,'1920x1080','course','unused','Flutter开发入门教程', '2026-05-10 19:00:00'),
(12,'Web性能优化','web-performance.mp4','legacy-performance-01','/file/profile/media/web-performance.mp4','MP4',207618048,2055,'1280x720','course','unused','Web性能优化技巧', '2026-05-10 20:00:00')
ON DUPLICATE KEY UPDATE
`media_name`=VALUES(`media_name`),`file_name`=VALUES(`file_name`),`file_url`=VALUES(`file_url`),
`format`=VALUES(`format`),`size_bytes`=VALUES(`size_bytes`),`duration_seconds`=VALUES(`duration_seconds`),
`resolution`=VALUES(`resolution`),`media_type`=VALUES(`media_type`),`status`=VALUES(`status`),`description`=VALUES(`description`);
