-- 智问学伴：学科领域技术字典与知识点先修拓扑图谱表迁移
-- 彻底消除 Java 代码中的硬编码学科黑白名单与先修关联规则

SET NAMES utf8mb4;

-- 1. 创建学科领域技术字典表 (edu_discipline_taxonomy)
CREATE TABLE IF NOT EXISTS `edu_discipline_taxonomy` (
  `id` bigint NOT NULL COMMENT '主键 ID，雪花算法',
  `domain_code` varchar(50) NOT NULL COMMENT '领域唯一标识编码，如 JAVA_BACKEND, FRONTEND, BIG_DATA, AI_LLM, GO_CLOUD_NATIVE, MOBILE',
  `domain_name` varchar(100) NOT NULL COMMENT '领域中文名称，如 Java 后端开发, Web 前端工程',
  `allowed_category_ids` varchar(255) NOT NULL DEFAULT '[]' COMMENT '允许召回的课程分类 ID 列表 JSON 数组，如 [2, 4]',
  `whitelist_keywords` text DEFAULT NULL COMMENT '白名单技能关键词列表 JSON 数组',
  `blacklist_keywords` text DEFAULT NULL COMMENT '强排他黑名单关键词列表 JSON 数组',
  `target_role_patterns` text DEFAULT NULL COMMENT '目标岗位匹配词条列表 JSON 数组',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否为全局默认兜底领域 (1:是, 0:否)',
  `sort_num` int NOT NULL DEFAULT 0 COMMENT '排序权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_code` (`domain_code`, `del_flag`),
  KEY `idx_sort_num` (`sort_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学科领域技术字典与边界隔离表';

-- 2. 创建知识点先修前驱关系表 (edu_knowledge_prerequisite)
CREATE TABLE IF NOT EXISTS `edu_knowledge_prerequisite` (
  `id` bigint NOT NULL COMMENT '主键 ID，雪花算法',
  `domain_code` varchar(50) DEFAULT NULL COMMENT '所属领域编码 (可为空代表跨领域通用)',
  `concept_name` varchar(100) NOT NULL COMMENT '后置核心概念/课程技能词，如 SpringCloud微服务, Kubernetes, Redis高并发缓存',
  `prerequisite_concept` varchar(100) NOT NULL COMMENT '必须前置修读的先修概念，如 SpringBoot基础, Docker容器, SQL基础',
  `strength_weight` decimal(3,2) NOT NULL DEFAULT 0.85 COMMENT '依赖强弱权重 (0.00 ~ 1.00，越高代表越不可跳过)',
  `rationale` varchar(255) DEFAULT NULL COMMENT '先修依赖教学法依据说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_concept_name` (`concept_name`),
  KEY `idx_prereq_concept` (`prerequisite_concept`),
  KEY `idx_domain_code` (`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点先修前驱依赖图谱表';

-- 3. 预置 6 大真实学科领域技术字典元数据
INSERT INTO `edu_discipline_taxonomy` 
(`id`, `domain_code`, `domain_name`, `allowed_category_ids`, `whitelist_keywords`, `blacklist_keywords`, `target_role_patterns`, `is_default`, `sort_num`, `create_time`, `update_time`, `del_flag`, `version`)
VALUES
(101, 'JAVA_BACKEND', 'Java 后端架构', '[2, 4]', 
 '["java", "spring", "springboot", "springcloud", "mybatis", "mysql", "redis", "kafka", "rabbitmq", "rocketmq", "jvm", "linux", "微服务", "高并发", "分布式", "中间件", "网络编程", "数据结构", "算法", "操作系统", "计算机网络", "设计模式", "netty", "juc", "seata", "ddd", "maven", "tomcat", "sharding", "后端", "数据库", "sql"]',
 '["go 语言", "go语言", "golang", "goroutine", "kratos", "geecache", "go后端", "go web", "gin框架", "gin ", "grpc", "protobuf", "rust", "kubernetes", "k8s", "terraform", "istio", "service mesh", "iac", "大数据", "hadoop", "flink", "spark", "datax", "sqoop", "clickhouse", "hive", "hbase", "数仓", "离线计算", "流批一体", "doris", "python", "django", "fastapi", "flask", "nlp", "word2vec", "大模型", "llm", "rag", "langchain", "pytorch", "tensorflow", "深度学习", "机器学习", "vue", "react", "typescript", "javascript", "html/css", "flutter", "android", "ios", "鸿蒙", "harmonyos", "harmony", "安卓", "c++20", "c++", "cpp", "node.js", "nodejs", "express", "koa", "区块链", "solidity", "游戏开发", "unity", "unreal", "渗透测试"]',
 '["java", "后端", "服务端", "后台", "架构", "架构师", "spring"]', 
 1, 10, NOW(), NOW(), 0, 0),

(102, 'FRONTEND', 'Web 前端工程', '[1]',
 '["vue", "vue3", "react", "typescript", "javascript", "前端", "html", "css", "web", "next.js", "vite", "webpack", "pinia", "vue-router", "tailwind", "element-plus", "node.js"]',
 '["java", "spring", "rust", "golang", "大数据", "hadoop", "flutter", "鸿蒙", "solidity", "unity"]',
 '["前端", "web", "vue", "react", "html", "h5", "js", "ts"]',
 0, 20, NOW(), NOW(), 0, 0),

(103, 'BIG_DATA', '大数据流批一体', '[7, 4]',
 '["大数据", "hadoop", "flink", "spark", "datax", "sqoop", "clickhouse", "hive", "hbase", "数仓", "数据开发", "mysql", "doris", "kafka", "scala", "etl", "数据湖", "iceberg"]',
 '["vue", "react", "前端", "flutter", "ios", "安卓", "鸿蒙", "solidity", "unity"]',
 '["大数据", "数仓", "spark", "flink", "数据开发", "etl", "数据架构"]',
 0, 30, NOW(), NOW(), 0, 0),

(104, 'AI_LLM', 'AI 与大模型算法', '[6]',
 '["ai", "人工智能", "大模型", "llm", "rag", "langchain", "nlp", "word2vec", "pytorch", "tensorflow", "深度学习", "机器学习", "python", "agent", "prompt", "向量数据库", "知识图谱"]',
 '["vue", "react", "前端", "flutter", "安卓", "区块链", "unity"]',
 '["大模型", "大语言模型", "语言模型", "llm", "ai大模型", "人工智能", "nlp", "算法工程", "深度学习", "算法工程师"]',
 0, 40, NOW(), NOW(), 0, 0),

(105, 'GO_CLOUD_NATIVE', 'Go 云原生架构', '[2, 5, 4]',
 '["go", "golang", "goroutine", "k8s", "kubernetes", "docker", "云原生", "gin", "kratos", "grpc", "protobuf", "linux", "devops", "istio", "prometheus"]',
 '["vue", "react", "前端", "rust", "python", "大数据", "hadoop"]',
 '["go", "golang", "云原生", "k8s", "容器", "微服务架构(go)"]',
 0, 50, NOW(), NOW(), 0, 0),

(106, 'MOBILE', '移动端与鸿蒙生态', '[3]',
 '["flutter", "android", "ios", "鸿蒙", "harmonyos", "harmony", "安卓", "移动端", "dart", "swift", "kotlin", "arkts"]',
 '["大数据", "hadoop", "spark", "clickhouse", "solidity", "k8s"]',
 '["移动", "flutter", "安卓", "android", "ios", "鸿蒙", "harmony"]',
 0, 60, NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE
`domain_name` = VALUES(`domain_name`),
`allowed_category_ids` = VALUES(`allowed_category_ids`),
`whitelist_keywords` = VALUES(`whitelist_keywords`),
`blacklist_keywords` = VALUES(`blacklist_keywords`),
`target_role_patterns` = VALUES(`target_role_patterns`),
`is_default` = VALUES(`is_default`),
`sort_num` = VALUES(`sort_num`),
`update_time` = NOW();

-- 4. 预置真实知识点先修前驱图谱边关系 (Prerequisite Directed Graph)
INSERT INTO `edu_knowledge_prerequisite`
(`id`, `domain_code`, `concept_name`, `prerequisite_concept`, `strength_weight`, `rationale`, `create_time`, `update_time`, `del_flag`, `version`)
VALUES
(1001, 'JAVA_BACKEND', 'SpringCloud微服务', 'Java核心语法', 0.95, '掌握面向对象与语法是框架学习不可违背的基础底座', NOW(), NOW(), 0, 0),
(1002, 'JAVA_BACKEND', 'SpringCloud微服务', 'SpringBoot基础', 0.95, '微服务组件治理必须建立在 SpringBoot 自动装配与起步依赖之上', NOW(), NOW(), 0, 0),
(1003, 'JAVA_BACKEND', 'Redis高并发缓存', 'SQL基础与关系型数据库', 0.85, '需先理解传统磁盘关系型持久化原理，才能体会内存键值缓存的必要性', NOW(), NOW(), 0, 0),
(1004, 'JAVA_BACKEND', 'Redis高并发缓存', '计算机网络基础', 0.80, '网络IO多路复用模型是精通 Redis 性能瓶颈排查的关键先修', NOW(), NOW(), 0, 0),
(1005, 'JAVA_BACKEND', '分库分表与MySQL调优', 'MySQL基础与索引原理', 0.90, '单表索引树原理未透彻掌握前，过早进行分布式分库分表会破坏认知渐进', NOW(), NOW(), 0, 0),
(1006, 'JAVA_BACKEND', 'JVM性能监控与底层调优', 'Java核心语法', 0.90, '需熟练掌握内存引用模型与对象生命周期', NOW(), NOW(), 0, 0),
(1007, 'JAVA_BACKEND', '分布式事务Seata实战', 'SpringCloud微服务', 0.85, '分布式事务是跨服务调用后的衍生痛点', NOW(), NOW(), 0, 0),
(1008, 'JAVA_BACKEND', 'Netty高性能网络编程', 'Java核心语法与网络编程', 0.90, 'NIO多路复用与Buffer机制是Netty前置依赖', NOW(), NOW(), 0, 0),
(1009, 'GO_CLOUD_NATIVE', 'Kubernetes容器集群治理', 'Docker容器基础', 0.95, 'K8s 是多容器编排管理系统，必须先通晓容器底层镜像构建', NOW(), NOW(), 0, 0),
(1010, 'GO_CLOUD_NATIVE', 'Kubernetes容器集群治理', 'Linux系统管理与网络基础', 0.90, '容器网络与系统内核调用为底层支撑', NOW(), NOW(), 0, 0),
(1011, 'AI_LLM', '大模型Agent智能体编排', 'Python核心编程', 0.95, 'Python 为主流大模型SDK与生态支撑语言', NOW(), NOW(), 0, 0),
(1012, 'AI_LLM', '大模型Agent智能体编排', '大模型Prompt工程与基础概念', 0.90, '需先理解基础生成机制再进行智能体多轮规划', NOW(), NOW(), 0, 0),
(1013, 'AI_LLM', 'RAG知识库向量检索', '向量数据库基础', 0.85, '向量语义相似度与嵌入矩阵是RAG核心组件', NOW(), NOW(), 0, 0),
(1014, 'FRONTEND', 'Vue3企业级项目实战', 'HTML5与CSS3基础', 0.95, 'DOM渲染与样式布局基础', NOW(), NOW(), 0, 0),
(1015, 'FRONTEND', 'Vue3企业级项目实战', 'ES6+与TypeScript基础', 0.95, 'Vue3 基于 Composition API 与 TS 深度构建', NOW(), NOW(), 0, 0),
(1016, 'BIG_DATA', 'Flink实时流计算实战', 'Java核心语法', 0.90, 'Flink计算算子依赖 Java/Scala 编程实现', NOW(), NOW(), 0, 0),
(1017, 'BIG_DATA', 'Flink实时流计算实战', 'SQL复杂分析查询', 0.90, '流式批式处理需具备严密的关系代数表达能力', NOW(), NOW(), 0, 0),
(1018, 'BIG_DATA', 'Spark海量数据离线批处理', 'Hadoop生态体系与HDFS', 0.85, '分布式存储基石与计算调度认知', NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE
`strength_weight` = VALUES(`strength_weight`),
`rationale` = VALUES(`rationale`),
`update_time` = NOW();
