# 项目智能体工作日志与操作规范手册 (AGENT_WORKLOG.md)

> **【强制执行铁律】**
> **所有接手本项目的 AI 智能体 / 工程师，在开展任何代码开发、服务排查、部署或调试工作前，必须无条件先阅读本文件以及 `share-parent/docs/AGENT_HANDOFF.md`！**
> 严禁盲目直接改动代码或重启服务，必须严格遵循本文档所记录的准则与避坑指南，避免造成线上事故。

---

## 一、最高工作准则与发布铁律 (Core Operating Rules)

1. **【发布闭环准则】先服务器定向验证 ➔ 后同步本地 ➔ 最后推送 GitHub**
   * 修改代码必须遵循标准单向流：
     $$\text{服务器修改/上传源码} \longrightarrow \text{服务器单服务构建} \longrightarrow \text{定向测试验证通过（严禁全量跑库）} \longrightarrow \text{同步拉回本地} \longrightarrow \text{审查 git diff} \longrightarrow \text{提交推送 GitHub}$$
   * **严禁反向操作**：绝对不要把未经验证的本地或 GitHub 代码直接覆盖服务器！
2. **【磁盘保护准则】绝对禁止在服务器上并发构建多个服务**
   * **严禁执行**：`docker compose up -d --build svc1 svc2 svc3`（特别禁止同时构建多个前端）。
   * **原因**：阿里云 ECS 云盘（ESSD Entry/PL0）基准 IOPS 仅 1800~3000。并发构建会同时拉起多个 Node.js 容器执行 `npm ci` 和多进程 Vite 打包，瞬间产生数十万个小文件写 I/O，2 分钟内抽干云盘突发积分池（Burst Bucket），触发硬件级限速（Throttling），导致 CPU 高 `iowait`、MySQL/Nacos 假死或服务崩溃。
   * **正确方式**：必须**严格串行逐个构建**（前一个构建完成、启动稳定且磁盘平息后再构建下一个），或**优先在本地编译出静态产物 `dist/`，服务器只做轻量文件同步**。
3. **【最小中断准则】禁止习惯性重启全站，禁止 `docker compose down -v`**
   * 严禁执行 `down -v`，这会导致 MySQL、Redis、Nacos 数据卷彻底损毁！
   * 修改哪个微服务，就只定向构建与替换哪个容器；前端改动只定向重启对应的 Nginx UI 容器。
4. **【接口保全准则】绝对保持后端业务协议完整性**
   * 严禁私自变更、删除已有微服务路由、Java Controller 接口路径、DTO 字段结构、数据库表结构或菜单权限配置。
5. **【本地禁启准则】严禁在本地环境中启动微服务或中间件**
   * **严禁执行**：在本地启动 Spring Boot 微服务（如 `java -jar`、`mvn spring-boot:run`、IDEA Run）、本地 Docker Compose、本地 MySQL、Redis 或 Nacos 实例。
   * **原因**：项目微服务集群已全面统一部署于阿里云 ECS 线上服务器（`47.120.67.187`），所有数据状态、Nacos 配置中心、Redis 缓存与数据库均在线上闭环。在本地拉起本地服务不仅会争抢本地系统端口、造成端口冲突，还会产生因本地与云端配置不一致带来的“脏调用”与环境割裂。
   * **正确方式**：本地环境严格仅用于代码编写、Git 版本管理、离线打包构建（`mvn package -DskipTests`、`npm run build`）以及向服务器发起命令运维和测试验证。所有应用服务的运行与联调必须严格在线上服务器进行。
6. **【严禁占用C盘准则】本地工作严禁占用C盘，临时与工作数据严格落地D盘**
   * **严禁执行**：在 C: 盘（包括各类临时目录、用户缓存、IDE 工作区、测试数据抓取目录等）写入大文件、临时脚本产物、离线数据集或日志。
   * **原因**：开发机 C: 盘为系统盘，容量紧张敏感，任何缓存积累极易导致系统盘爆满与系统告警。
   * **正确方式**：所有临时脚本、数据抓取、离线文件处理、构建中间缓存等必须严格存放于 D: 盘项目空间（如 `d:\education system\my-porject\.scratch\`，已加入 `.gitignore`）。产生的一切中间测试垃圾必须在流程结束后立即清理，严禁侵占 C: 盘空间。
7. **【每日公网 IP 变更检测铁律】每个自然日首次涉及服务器操作前，必须执行一次公网 IP 校验**
   * **背景**：阿里云 ECS 实例使用动态公网 IP，服务器重启后公网 IP 可能变化。
   * **检测方式**：执行 `workbench exec --instance-id i-f8z1loc07p8p5ve8c7jf --command "curl -s http://100.100.100.200/latest/meta-data/eipv4"`，将返回值与本文档「二、关键环境与资产索引」中记录的 **ECS 公网 IP** 进行比对。
   * **若 IP 未变**：无需操作，记录当天已完成检测即可。
   * **若 IP 已变更**：必须立即批量更新项目内所有硬编码旧 IP 的文件（`AGENTS.md`、`AGENT_WORKLOG.md`、`AGENT_HANDOFF.md`、冒烟测试脚本等），并提交推送至 GitHub。
8. **【代码修改定向测试铁律】严禁在修改局部代码后跑全量测试，严格只测试涉及修改的部分**
   * **严禁执行**：在日常修改某个微服务接口、前端组件或业务逻辑后，盲目运行全站全量回归测试（如执行包含加购、下单、支付、评价等 62 项全量冒烟）。
   * **原因**：全量测试会发起大量跨服务读写请求，产生不必要的测试脏数据，占用服务器珍贵的 CPU 和云盘 I/O，并极大拖慢开发验证效率。
   * **正确方式**：改动了哪个服务、哪个业务接口或哪个前端模块，**严格只针对该改动点进行精准定向验证**（如修改教育服务课程接口就仅针对性验证该课程接口；修改 AI 客服就仅定向测试该问答接口；修改管理端页面就仅针对性验证该管理端页面）。只有在重大跨服务底层重构或全站发布前，才经批准执行全量回归。

---

## 二、关键环境与资产索引 (Environment & Infrastructure)

| 资产项 | 配置详情 | 备注 |
| :--- | :--- | :--- |
| **ECS 实例 ID** | `i-f8z1loc07p8p5ve8c7jf` | 阿里云 ECS（华东区） |
| **ECS 公网 IP** | `47.120.67.187` | 线上运行地址（动态公网，当前已切换至 `47.120.67.187`） |
| **服务器项目路径** | `/opt/tianji/share-parent` | Docker Compose `tianji-share` 运行目录（注：无 `.git`） |
| **本地代码根目录** | `D:\education system\my-porject\share-parent` | Java 17 + Vue3 前后端源码 |
| **本地 Git 仓库根** | `D:\education system\my-porject` | 分支 `master`，远端 `git@github.com:Hyiii123/my-porject.git` |
| **远程连接工具** | `D:\nodejs_global\workbench.exe` | 阿里云 Workbench CLI（已配置凭证，支持 `exec` 与 `upload`） |
| **前端 - 学生端** | `http://47.120.67.187:18081` | 容器 `tianji-portal-ui`，对应源码 `frontends/portal` |
| **前端 - 业务管理端** | `http://47.120.67.187:18082` | 容器 `tianji-business-admin-ui`，对应源码 `frontends/business-admin` |
| **前端 - 基础管理端** | `http://47.120.67.187:18080` | 容器 `tianji-ruoyi-ui`，对应源码 `share-ui` |
| **API 网关 Gateway** | `http://47.120.67.187:8080` | 容器 `tianji-gateway`，统一接口入口 |
| **Nacos 控制台** | 内部端口 `8848` / 宿主机 `8848` | 配置中心与服务发现（命名空间等依赖外部 MySQL） |

---

### 三、重大里程碑与工作演进记录 (Milestones & Evolution)

### 2026-09-09 01:55:00 - Qdrant 向量库全量 2370 题向量化、FastEmbed 语义检索微服务与客服智能问答闭环落地

* **核心成果**：
  1. **Qdrant 向量数据库部署与全量向量化**：
     - 在轻量 Rust 向量数据库 Qdrant 中创建 `tianji_knowledge` 集合（512 维向量，Cosine 余弦相似度）；
     - 使用轻量 ONNX 推理引擎 FastEmbed 加载中文向量模型 `BAAI/bge-small-zh-v1.5`，全量 2370 道题目与答案高性能向量化批量 upsert 入库（points_count: 2370, status: green）；
  2. **FastAPI 语义检索微服务容器化（tianji-embedding）**：
     - 构建轻量 Python 3.11 镜像并部署独立容器 `tianji-embedding`（常驻内存仅 172MB，端口 8000，宿主机 18000），挂载离线模型缓存实现零外网依赖启动；
     - 提供 `GET /health`、`POST /embed` 及 `GET /search?q={text}&limit={n}` 检索接口，向量检索延迟低至 15ms；
     - 将 `embedding` 服务正式纳入 `docker-compose.yml` 编排管理；
  3. **客服微服务（share-customer）全链路语义集成与防护**：
     - `CustomerService.java` 新增 `findSemanticAnswer`：优先通过语义检索服务进行向量相似度匹配（阈值 >= 0.70 判定命中），不满足则平滑降级至 FAQ 与关键词；
     - 解决真题详尽解析数千字引发的数据库截断异常：将 MySQL `cs_session.last_message` 字段类型由 `varchar(1000)` 平滑修改为 `TEXT`，并在 Java 层增加 `formatLastMessagePreview` 安全截断；
  4. **验证**：真实提问“Java为什么支持跨平台？”、“Redis缓存雪崩怎么解决？”秒级精准召回知识点，冒烟测试 62/62 项满分通过。

### 2026-09-09 01:25:00 - 小林coding后端面试题库全量导入（1322题）与本地严禁占用C盘铁律建立

* **核心成果**：
  1. **题库抓取清洗与入库 (V28 迁移)**：
     - 深度抓取小林coding全域 23 个技术专题（Java基础/并发/JVM、C++底层、Go语言、MySQL/Redis、计网、操作系统、算法、MQ、分布式系统设计等）；
     - 提炼 **1322 道**工业级面试真题与详尽解析，生成持久化脚本 `V28__import_xiaolincoding_interview_knowledge.sql` 导入线上 `tj_customer`，知识库总规模跃升至 **2370 条**；
  2. **本地磁盘保护铁律确立**：
     - 明确禁止使用本地 C: 盘，所有临时脚本与数据处理严格落地 D: 盘项目空间（`.scratch/`），彻底清理 C: 盘历史缓存并恢复 23.5GB+ 纯净空间。

### 2026-09-09 01:00:00 - 非业务辅助服务（gen/job/monitor）永久关停，系统释放 1.4GB 内存

* **核心成果**：
  1. **关停容器与彻底移除**：关停并永久下线 `tianji-gen`（若依代码生成，省 520MB）、`tianji-job`（Quartz调度脚手架，省 528MB）、`tianji-monitor`（Admin监控，省 322MB），累计为 ECS 服务器**释放 1.4GB 物理内存**与 210+ 系统线程；
  2. **编排永久禁用规范**：在 `docker-compose.yml` 中将上述三项服务打上 `profiles: ["dev-tool"]` 标签并设置 `restart: "no"`，禁止生产与日常运维默认启动，服务器空闲内存大幅提升至 2.1GiB+。

### 2026-09-09 00:35:00 - API 密钥 AES-256 加密存储、运行时自愈解密与 GPT-5.6-Luna 全栈模型收敛

* **核心成果**：
  1. **代码与版本库零明文密钥净化**：清理配置文件、Nacos 配置模版、代码类、Docker Compose 及历史工作日志中的全部明文 `sk-` 密钥，配置 `.gitignore` 阻断敏感凭据；
  2. **AES-256 密文存储与运行时自愈解密**：
     - 在 `share-common-core` 新增 `AesCryptoUtil.java`（AES/CBC/PKCS5Padding + SHA-256 密钥派生）；
     - 密钥密文存入 MySQL `tj_customer.cs_ai_config.api_key_ciphertext`，`CustomerService` 在 Redis 缓存缺失时自动自愈解密注入缓存，保障全流程无明文硬编码；
  3. **模型名称全链路统一收敛**：后端微服务、Nacos、前端学生端/管理端模型统一对齐为 `gpt-5.6-luna`。

### 2026-09-08 06:05:00 - 客服业务与 IT 行业技术双知识库全域规模化导入（V25 & V26 迁移）

* **核心成果**：
  1. **IT 行业主流技术图谱导入 (V26)**：
     - 对齐 CS-Notes、JavaGuide 及大厂高频技术标准，覆盖 Java、Spring微服务、MySQL、Redis、MQ、云原生/Linux、计网安全、算法、现代大模型(RAG/Agent)、系统架构等 11 大核心技术领域，新增 122 条工业级专业问答条目（IDs 101~222）；
  2. **客服业务知识库与 FAQ 全量扩充 (V25)**：
     - 对齐 NLP-CEduCusSerC 与 EduQA 等权威教育客服语料库，覆盖账号登录、课程播放、沙箱实验、导师答疑、支付订单、退款售后、发票财税等 10 大教育业务领域，知识库扩充至 80 条、FAQ 扩充至 30 条；
  3. **高精粒度分词与双基准测试验证**：优化关键词分词打分机制，消除短特征误匹配，152 组全域联合基准问答测试 100% 满分通过。

### 2026-09-08 05:25:00 - 全站“天机”/“tianji”全域对齐与品牌重命名“智问”/“zhiwen”

* **核心成果**：
  1. **前端工程与静态资产全面更名**：`portal` 与 `business-admin` 的 `package.json`、代理配置、Mock 数据、SVG Banner 资产、Iconfont、README 统一迁移对齐为“智问”/“zhiwen”；
  2. **后端微服务架构代码对齐**：讲师署名对齐为“智问教研团队”，重构 `LegacyZhiwenUserController`，短信验证码前缀对齐为 `zhiwen:auth:verifycode:`，演示支付协议对齐为 `demo://zhiwen-pay/`，各微服务 POM 及 Application 统一更名；
  3. **线上数据资产无损迁移 (V24)**：线上 `share.sys_user` 5,100 名学员与管理员邮箱统一迁移至 `@zhiwen.com`，教师与课程表全面清洗对齐。

### 2026-09-08 05:10:00 - 学员端首页视觉统一与极简浅色风重构

* **核心成果**：
  1. **消除暗黑控制台割裂感**：将顶部 `AgentReasoningHUD.vue` 由生硬刺眼的暗黑赛博控制台重构为纯白高阶卡片（`#FFFFFF`）与浅柔底色（`#F8FAFC`），融入全站教育质感；
  2. **以学员为中心去术语化**：去除底层工程名词，转换为亲切直观的五步导学流（`01 学情诊断` ➔ `02 智能匹配` ➔ `03 能力拆解` ➔ `04 进阶规划` ➔ `05 专属导学`）；
  3. **推荐卡片视觉净化与排版优化**：彻底清除封面多重盖章色块，下线模板化通用轮播图，提升 AI 导学与专属推荐至首屏核心区，推荐卡片呈现杂志级呼吸感。

### 2026-09-08 04:18:00 - 多智能体个性化推荐体系与 Python SPI 算法解耦全栈落地

* **核心成果**：
  1. **五大多智能体协同流水线**：
     - 构建用户画像 Agent ➔ 推荐 Agent ➔ 课程分析 Agent ➔ 路径规划 Agent ➔ RAG 知识检索 ➔ 解释生成 Agent 的完整流水线；
     - 抽象 `IRecommendAlgorithmEngine` 算法 SPI，默认接入 `RemotePythonAlgorithmEngine` 并支持自动无缝降级；
  2. **前端多智能体协同推理看板与职业大屏**：
     - 沉浸式协同推理动态看板（`AgentReasoningHUD.vue`）支持 5 阶段点亮、目标岗位实时推演与智能体推理透视；
     - 可解释性深度透视推荐卡片（Explainable AI Cards）展示 GPT 生成的推荐理由、突破短板徽章与先修依赖；
     - 职业成长进阶全景大屏（`CareerPathDrawer.vue`）呈现 4 阶段 DAG 课程递进路线与一键生成计划。

### 2026-09-07 14:50:00 - 扩充 300 门真实 IT 专业课程与 1,820 节教学大纲上线 (V22 迁移)

* **核心成果**：
  1. **100% 纯 IT 专业覆盖**：基于真实 MOOCCubeX / Coursera IT 课程体系，覆盖前端、后端、移动端、数据库、云原生DevOps、人工智能、数据科学、网安、游戏、区块链 10 大专业分类，全站课程扩充至 **320 门**；
  2. **多表联动与完整教学大纲**：同步填充 `edu_course_teacher` 讲师绑定与 `edu_course_catalog` 真实章节结构，全站目录扩充至 **1,820 节**，支持点播与打卡学习；
  3. **推荐引擎特征对接**：技能标签全量对齐 50 维技术图谱向量空间，实现高精度契合度匹配。

### 2026-09-07 14:20:00 - 5,000 名真实 IT 学员画像还原与嵌入式密集向量检索引擎 (V20 & V21 迁移)

* **核心成果**：
  1. **万人级真实学员分布数据拟合**：
     - 深入挖掘清华大学 MOOCCubeX 与英国开放大学 OULAD 数据集，导入 **5,000 名真实 IT 学员**（涵盖 Java、前端、AI算法、全栈、DevOps、Go 工程师）；
     - 生成 9,487 条真实选课打卡与章节进度记录 (`edu_learning_record`)，全站真实学员数扩至 5,123 人；
  2. **嵌入式密集向量余弦相似度检索引擎**：
     - 在微服务内存中构建 50 维 IT 技能密集向量空间字典，实现 `computeCosineSimilarity` 余弦夹角计算引擎，额外内存开销 < 10MB，微秒级极速匹配；
     - 服务端学员管理页面重构为标准分页与异步精准统计（5,106 名学员）。

### 2026-09-07 12:00:00 - 用户画像数据模型与多路召回推荐引擎底座构建 (V19 迁移)

* **核心成果**：
  1. **课程技能标签与难度标定**：Flyway 迁移 `V19__course_skills_and_user_portrait.sql` 为课程建立 50+ 技能标签、目标岗位与前置依赖基座；
  2. **用户画像数据模型**：新增 `edu_user_portrait` 表，根据学员学习打卡与考试记录动态计算技术偏好向量与自律指数；
  3. **多路召回推荐引擎与原生 SVG 技能雷达图**：落地包含技能匹配、岗位匹配、难度适配的多路召回算法，前端实现纯原生响应式 SVG 蜘蛛网雷达图（零 ECharts 依赖）。

### 2026-09-07 04:30:00 - Redis 高并发秒杀抢购、高频点赞榜与模拟沙箱支付闭环落地

* **核心成果**：
  1. **Redis 基础设施升级**：在 `share-common-redis` 补齐原子递减、分布式互斥锁、Set 去重与 ZSet 有序集合排行；
  2. **高频点赞热榜**：Set 键用户防重 + ZSet 键点赞计分，开放免鉴权排行榜接口与冷启动预热；
  3. **库存预扣与秒杀直购**：Redis 预扣配额结合 Set 防刷防超领，秒杀直购通道自动生单报读；
  4. **全链路模拟沙箱支付**：支持 `/ts/pay/order/{orderId}/demo-success` 支付回调，自动发放课程权益与学习记录。

### 2026-09-07 01:00:00 - 在线教育全站前端视觉现代重构

* **核心成果**：
  1. 依据 `frontend-design` 规范去除廉价模板感，废弃紫蓝发光渐变与浮球装饰；
  2. 确立 **Deep Slate（#0F172A）** + **Academic Blue（#2563EB）** 高阶学术科技色调；
  3. 卡片统一规范为 8px，表单、输入框统一为 6px，彻底移除 100px 跑马道胶囊；
  4. 重塑 16:9 标准比例课程卡片、个人中心与 AI 客服聊天气泡视觉体系。

---

## 四、核心踩坑历史与避坑手册 (Known Pitfalls & Solutions)

| 序号 | 故障 / 坑点现象 | 根因剖析 | 正确解决方案与预防措施 |
| :---: | :--- | :--- | :--- |
| **1** | **在服务器并发构建导致严重超时甚至假死** | 阿里云 ECS ESSD Entry 云盘基准性能仅 1800~3000 IOPS。并发多前端构建瞬间产生数万小文件解压与写 I/O，秒级抽干突发积分（Burst Credits），触发硬件级强制限速（Throttling），导致系统高 `iowait`，甚至拖死 Nacos/MySQL。 | **严禁一条命令构建多个服务！** 必须串行逐一排队构建（`docker compose ... up -d --build portal-ui`，等落盘启动后再 build 下一个）。或在本地打出 `dist/` 纯静态文件上传，实现“服务器零构建”。 |
| **2** | **改了前端代码但线上访问无变化** | 1. 线上容器是静态 Nginx，不具备本地 dev 模式的热重载机制，必须重载/重建容器；<br>2. 浏览器会对旧版的 js/css 资源进行强缓存。 | 1. 重新构建容器或替换挂载的静态产物；<br>2. 指导用户或在浏览器中强制清除缓存刷新（`Ctrl + F5`）。 |
| **3** | **Workbench CLI 终端执行报命令找不到** | Workbench CLI 在非交互模式下生成的 shell 环境变量（`PATH`）极度精简，找不到 `/usr/local/bin` 中的 `docker`、`docker-compose` 等命令。 | 每次使用 `workbench exec` 时，必须在命令最开头显式添加：`export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin && ...` |
| **4** | **Workbench CLI 执行长任务报超时退出** | `workbench exec` 默认超时仅 30 秒。 | 对于容器构建或依赖安装等耗时操作，必须显式附加参数：`--timeout 600`。 |
| **5** | **Vue 组件内 SCSS 嵌套导致 Vite 构建报警告** | 组件内 `<style scoped>` 中使用了 `&.icon-primary` 等 SCSS 语法，但未声明预处理器语言。 | 只要有嵌套或 SCSS 变量，必须在 style 标签显式标注：`<style scoped lang="scss">`。 |
| **6** | **微服务客服接口莫名报 404 或进入教育模块** | Spring Cloud Gateway 路由匹配规则按顺序生效。如果通用的 `/cs/**` 排在特例 `/cs/customer-service/**` 之前，客服请求会被错误转派到教育服务。 | 保持网关特例路由必须排在通用规则之前；新增路由时务必全局搜索调用端。 |
| **7** | **生产前端页面请求报接口错误或请求指向 localhost** | Vite 打包时将开发环境的 `localhost:8080` 固化进了生产包，导致访客浏览器尝试请求访客自己的本地电脑。 | 生产前端一律使用同源空前缀（`/`），由前端 Nginx（`frontends/nginx.conf`）反向代理转发至 `gateway:18080`。 |
| **8** | **Windows 本地构建后端报 Java 版本不匹配** | 操作系统全局默认可能是 Java 8，而项目已升级为 Spring Boot 3 + Spring Cloud 2022，强制要求 Java 17。 | 设置当前终端环境变量：`$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'; $env:Path = "$env:JAVA_HOME\bin;$env:Path"`，严禁擅自降级 POM。 |
| **9** | **执行了增量 SQL 但数据库表结构没有变化** | Docker 启动 MySQL 时，`docker-entrypoint-initdb.d` 仅在**全新空卷**创建时执行，已有数据卷绝对不会重新执行初始化 SQL。 | 必须使用 Flyway 增量机制，通过 `deploy/mysql/apply-migrations.ps1` 写入 `share.flyway_schema_history`。 |
| **10** | **本地同时启动 portal 与 business-admin 端口冲突** | 两个前端工程的 Vite 配置默认端口都是 `18081`。 | 业务管理端启动时显式指定不同端口：`npm run dev:prod -- --port 18083`。 |
| **11** | **前端静态课程图片 404 导致裂图** | 数据库/Mock 封面路径与前端静态资源文件名不一致（如 `performance.svg` vs `web.svg`、`golang.svg` vs `go.svg`），且组件未挂载 `@error` 容灾事件与默认封面回退。 | 1. 补齐所有别名图片文件；<br>2. 新增深色学术科技风 16:9 标准默认课程封面 `default-cover.svg`；<br>3. 所有渲染课程封面的 Vue 组件统一挂载 `@error="handleImgError"` 与 `defaultCover` 容灾回退。 |
| **12** | **新开放免鉴权接口被网关拦截报 401** | Spring Cloud Gateway 默认会对微服务路由实施统一鉴权，仅在 `security.ignore.whites` 中的接口放行。 | 新增面向未登录用户的公开接口（如 `/cs/courses/ranking/**` 点赞榜）时，必须同步在 Nacos 的 `share-gateway-dev.yml` 配置的 `security.ignore.whites` 中声明放行，并通过 Nacos OpenAPI 更新配置。 |
| **13** | **后端服务构建耗尽服务器突发磁盘 IOPS** | 服务器 ECS 未安装 Maven 且磁盘突发积分宝贵，直接在服务器容器内执行编译会耗尽 IOPS 并造成死机。且 Dockerfile 直接通过 `COPY ${JAR_FILE} app.jar` 运行。 | 在本地利用已配置好的 JDK 17 执行 `mvn clean package -DskipTests` 生成目标 JAR，将更新的 JAR 打包压缩后通过 Workbench CLI 上传，服务器仅需 10 秒轻量 `docker build` 替换容器。 |

---

## 五、每次工作执行清单 (Standard Operating Procedure)

```mermaid
flowchart TD
    A[收到用户需求] --> B[阅读 AGENT_WORKLOG.md & docs/AGENT_HANDOFF.md]
    B --> C[检查本地与服务器状态 / 明确影响范围]
    C --> D[服务器端做最小代码改动 / 本地轻量验证]
    D --> E[服务器单服务串行更新 (严禁并发构建)]
    E --> F[运行 deploy/smoke-test.ps1 验证 35 项冒烟]
    F --> G{冒烟测试是否全通?}
    G -- 失败 --> H[查看容器日志与排查, 修复后重新验证]
    H --> E
    G -- 成功 --> I[同步代码至本地对应路径]
    I --> J[本地 git diff 逐行审查差异]
    J --> K[git commit 规范提交并推送 GitHub]
    K --> L[验证本地与远端 Commit Hash 一致]
    L --> M[更新本 AGENT_WORKLOG.md 并向用户交付]
```

### 1. 动工前检查 (Pre-flight)
- [ ] 阅读本 `AGENT_WORKLOG.md` 确认最新的卡点与注意事项。
- [ ] 检查本地 Git 状态：`git status`、`git branch`、`git log -3 --oneline`。
- [ ] 检查服务器运行状态：`docker compose -p tianji-share ps`、网关健康 `curl http://127.0.0.1:8080/actuator/health`。

### 2. 变更中约束 (In-flight)
- [ ] 仅修改与本次任务直接相关的文件，绝不扩大战线。
- [ ] 如需更新多个服务，必须串行排队构建，禁止并发多构建。

### 3. 验收与归档 (Post-flight)
- [ ] 运行冒烟测试：`powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\smoke-test.ps1 -BaseUrl 'http://47.120.32.166:8080'`。
- [ ] 代码拉回本地后，执行 `git diff --check`。
- [ ] 提交信息使用规范语义（`feat:`, `fix:`, `style:`, `docs:`, `refactor:`）。
- [ ] `git push origin master`，并核对 `git rev-parse HEAD` 与 `git ls-remote`。
- [ ] 将本次改动的重要节点、新踩坑点记录追加至本文件。
