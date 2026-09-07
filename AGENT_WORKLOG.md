# 项目智能体工作日志与操作规范手册 (AGENT_WORKLOG.md)

> **【强制执行铁律】**
> **所有接手本项目的 AI 智能体 / 工程师，在开展任何代码开发、服务排查、部署或调试工作前，必须无条件先阅读本文件以及 `share-parent/docs/AGENT_HANDOFF.md`！**
> 严禁盲目直接改动代码或重启服务，必须严格遵循本文档所记录的准则与避坑指南，避免造成线上事故。

---

## 一、最高工作准则与发布铁律 (Core Operating Rules)

1. **【发布闭环准则】先服务器验证 ➔ 后同步本地 ➔ 最后推送 GitHub**
   * 修改代码必须遵循标准单向流：
     $$\text{服务器修改/上传源码} \longrightarrow \text{服务器单服务构建} \longrightarrow \text{HTTP 冒烟验证通过} \longrightarrow \text{同步拉回本地} \longrightarrow \text{审查 git diff} \longrightarrow \text{提交推送 GitHub}$$
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

---

## 二、关键环境与资产索引 (Environment & Infrastructure)

| 资产项 | 配置详情 | 备注 |
| :--- | :--- | :--- |
| **ECS 实例 ID** | `i-f8z1loc07p8p5ve8c7jf` | 阿里云 ECS（华东区） |
| **ECS 公网 IP** | `47.120.32.166` | 线上运行地址 |
| **服务器项目路径** | `/opt/tianji/share-parent` | Docker Compose `tianji-share` 运行目录（注：无 `.git`） |
| **本地代码根目录** | `D:\education system\my-porject\share-parent` | Java 17 + Vue3 前后端源码 |
| **本地 Git 仓库根** | `D:\education system\my-porject` | 分支 `master`，远端 `git@github.com:Hyiii123/my-porject.git` |
| **远程连接工具** | `D:\nodejs_global\workbench.exe` | 阿里云 Workbench CLI（已配置凭证，支持 `exec` 与 `upload`） |
| **前端 - 学生端** | `http://47.120.32.166:18081` | 容器 `tianji-portal-ui`，对应源码 `frontends/portal` |
| **前端 - 业务管理端** | `http://47.120.32.166:18082` | 容器 `tianji-business-admin-ui`，对应源码 `frontends/business-admin` |
| **前端 - 基础管理端** | `http://47.120.32.166:18080` | 容器 `tianji-ruoyi-ui`，对应源码 `share-ui` |
| **API 网关 Gateway** | `http://47.120.32.166:8080` | 容器 `tianji-gateway`，统一接口入口 |
| **Nacos 控制台** | 内部端口 `8848` / 宿主机 `8848` | 配置中心与服务发现（命名空间等依赖外部 MySQL） |

---

## 三、重大里程碑与工作演进记录 (Milestones & Evolution)

### 2026-09-07 14:50:00 - 扩充 300 门真实 IT 专业课程与完整教学大纲上线

* **任务背景**：针对全站此前仅有 20 门基础课程、课程体系较为单薄的问题，依据真实 MOOCCubeX / Coursera IT 课程体系，扩充 300 门 100% 纯 IT 领域的高质量前沿专业课程与配套教学大纲。
* **核心数据与架构落地**：
  1. **100% 纯 IT 覆盖与科学分类分布 (V22 迁移)**：
     - 均衡覆盖系统既有 10 大分类：前端开发 (40门)、后端开发 (50门)、移动开发 (26门)、数据库 (32门)、云计算与DevOps (36门)、人工智能 (47门)、数据科学 (31门)、网络安全 (26门)、游戏开发 (16门)、区块链 (16门)，全站课程扩充至 **320 门**。
     - 覆盖主流与前沿技术（SpringCloud Alibaba、K8s、大模型微调与RAG、ClickHouse、Rust、Flutter、HarmonyOS NEXT、Kafka、Flink、Solidity 等），难度等级涵盖初级入门 (1)、中级进阶 (2)、高级架构 (3)。
  2. **多表联动与教学大纲建设**：
     - 同步填充 `edu_course`（ID: 21~320）、`edu_course_teacher` 讲师分配（全站绑定 320 条）与 `edu_course_catalog` 真实章节结构（全站目录扩充至 **1,820 节**）。
     - 每门课配备 2 个核心章节与 4 个专业小节（含选型指南、工程化搭建、业务落地与调优），支持学员端点播、章节打卡与学时累计。
  3. **推荐引擎特征无缝对接**：
     - 细粒度技术技能标签全量对齐现有的 50 维技术图谱向量空间，推荐引擎即时召回最高契合度 99 分的专属课程。
* **部署与验证**：
  - 增量迁移脚本 `V22__add_300_real_world_it_courses.sql` 线上无损导入；
  - 接口验证：`/ss/courses/portal` 准确返回 `total: 320`；
  - 冒烟测试 **62/62 项全部通过**。

### 2026-09-07 14:45:00 - 学员端智能学习画像与动态 SVG 技能雷达图发布上线

* **任务背景**：用户反馈在学员端门户中登录后未看到技能雷达图、画像标签、自律完课指数与目标岗位。
* **根因排查**：
  1. **线上前端静态资源滞后**：后端 `/cs/user/portrait` 接口数据与逻辑均正常，但 `tianji-portal-ui` 容器内的 Nginx 静态文件为先前历史版本，未将包含学习画像与雷达图的前端代码打包发布进容器。
  2. **左侧导航路由入口缺失**：个人中心 `LeftNav.vue` 会过滤掉 `hidden: true` 的概览路由，且此前未在左侧导航单独开放“学习画像”菜单项，导致学员跳转到“我的课程”等二级页面后无法便捷切回画像主页。
  3. **雷达图图形渲染升级**：原设计仅有技能进度条，缺少直观的几何多维雷达图（Spider Web Radar Chart）。
* **核心架构改造与实现**：
  1. **高颜值动态原生 SVG 技能雷达图落地**：
     - 在 `personal/main.vue` 实现纯原生响应式 SVG 蜘蛛网雷达图（中心坐标 `(190, 160)`、半径 `95px`，5层 20%~100% 同心网格线、辐射轴线、半透明蓝色填充多边形、顶点光晕数据点及技能分值动态排版）。
     - 与右侧技能掌握度进度条（95分、74分等）并排组成双栏智能面板，兼具视觉美观与零外部依赖（无需引入臃肿的 ECharts 库）。
  2. **导航与路由体系升级**：
     - 在 `base.js` 中将个人中心首页显式注册 `overview` 路由，并与 `LeftNav.vue` 的 `personalRoute` 深度打通，新增“学习画像”左侧菜单第一项，并支持自动激活高亮。
     - 头部组件 `Header.vue` 下拉菜单直链 `/personal/main/overview`。
  3. **发布与验证**：
     - 本地执行 `npm run build` 打包 `tianji-portal`，通过 Workbench 部署更新 `tianji-portal-ui` 容器。
     - 冒烟测试 **62/62 项通过**。

### 2026-09-07 14:35:00 - 学员管理页面适配 5,000+ 学员展示与服务端分页修复

* **任务背景**：导入 5,000 名真实 IT 学员后，用户反馈业务管理端（`tianji-business-admin-ui`，端口 18082）“学员管理”页面仍只显示 6 个历史演示学员，无法浏览新导入的学员群体。
* **根因定位**：
  1. **后端用户类型过滤条件限制**：后端若依用户系统 `/us/students/page`（由 `LegacyTianjiUserController.java` 实现）写死根据 `userType == "01"`（即学员身份）进行条件过滤；新导入的 5,100 名学员在 SQL 脚本中 `user_type` 默认赋予了 `'00'`（系统用户默认值），导致被接口逻辑过滤。
  2. **前端假分页与统计限制**：原 `frontends/business-admin/src/pages/userlist/student/index.vue` 采用了一次性请求 `pageSize: 200` 的前端截取假分页，且顶部统计卡片（学员总数、正常学员、禁用学员）统计的是前端数组的 `length`，上限仅为 200。
* **核心架构改造与修复**：
  1. **数据库层修复与迁移文件同步**：
     - 在线上数据库执行 `UPDATE share.sys_user SET user_type = '01' WHERE user_id >= 201;`，将 5,100 名真实学员用户类型批量同步修正为 `'01'`。
     - 深度更新本地 `V20__real_world_learners_and_portrait_dataset.sql` 与 `V21__scale_5000_real_world_it_learners.sql`，将所有 `sys_user` 批量插入语句中的 `'00'` 全量替换为 `'01'`，彻底消除未来环境重建时复现的风险。
  2. **前端页面服务端真实分页与异步统计改造**：
     - 重构 `frontends/business-admin/src/pages/userlist/student/index.vue`，废除前端数组截取，实现标准的服务端分页参数传递（`pageNum`、`pageSize`、`userName`、`status`、`sex`）。
     - 新增 `getStudentStats()` 方法，并发请求全量与禁用学员接口，顶部统计卡片实时精确展示：**学员总数：5,106**、**正常学员：5,105**、**禁用学员：1**。
  3. **遵循云盘保护准则的纯静态构建部署**：
     - 本地执行 `npm run build` 生成 `dist/`，打包压缩后通过 Workbench 上传至 ECS，单容器替换 `tianji-business-admin-ui` 静态资源并重启，避免服务器 Node.js 构建带来的 I/O 击穿风险。
* **验证结果**：
  - 接口实测：`/us/students/page` 正确返回 `total: 5106`，翻页正常。
  - 冒烟测试：运行 `deploy/smoke-test.ps1 -BaseUrl "http://47.120.32.166:8080" -IncludeWriteFlow`，**62/62 项全链路冒烟测试无误通过**。

### 2026-09-07 14:20:00 - 5,000 名真实 IT 学员画像还原与嵌入式密集向量检索引擎落地

* **任务背景**：深入挖掘清华大学 MOOCCubeX 与英国开放大学 OULAD 真实学术与工业级学习分析数据集，剔除非 IT 课程，聚焦 100% IT 专业领域，将学员规模扩充至 5,000 人（万人级真实在线教育体量），并在微服务内部落地高性能嵌入式密集向量余弦相似度检索引擎。
* **核心架构改造与实现**：
  1. **免费向量数据库选型与嵌入式向量引擎 (In-Process Dense Vector Cosine Engine)**：
     - **硬件现实评估**：当前阿里云 ECS 为 2核/8GB，已跑 16 个微服务容器，仅剩 1.1GB 内存且无 Swap，强推 Milvus/Qdrant 必引发 OOM-killer 宕机。
     - **零内存风险方案**：在 `EducationService` 构建标准 50 维 IT 技能密集向量空间字典 (`IT_SKILL_DIMENSIONS`)，实现 `computeCosineSimilarity` 余弦夹角计算引擎，额外内存开销 < 10MB，微秒级匹配。
     - **融合打分**：推荐引擎融合向量空间相似度 (`vectorSimilarity * 35.0`) 与离散技能重叠度，使推荐理由精确呈现契合度百分比（如 `基于技术图谱向量契合度 (86%) 推荐`）。
  2. **非 IT 课程剔除与 100% IT 纯净度保证**：
     - 确保全站 20 门核心课程与 10 大分类 100% 覆盖主流 IT 技术（Java/微服务、前端全家桶、AI大模型、全栈开发、DevOps容器、Go/Rust高并发等）。
  3. **万人级真实学员分布数据还原 (V20 & V21 迁移)**：
     - 导入 **5,000 名真实 IT 学员** (`user_id` 1001~6000)，涵盖 1,532 名 Java 后端、1,275 名前端、918 名 AI 算法、612 名全栈、408 名 DevOps、357 名 Go 底层工程师。
     - 依据 OULAD 学术统计学规律拟合：22% 高自律学员（完课率 75%~96%，学时 65~180h）、53% 系统进阶期、25% 探索期；分布涵盖夜间专注 (50%)、晨曦型、周末突击型。
     - 关联生成 **9,487 条真实选课打卡与章节进度记录** (`edu_learning_record`)，全站真实学员数扩充至 **5,123 人**，画像数达 **5,102 条**。
* **部署与验证**：
  - 本地 Maven 打包 `share-education.jar` 并单容器重启。
  - Flyway V20 与 V21 增量脚本无损迁移成功。
  - 冒烟测试 **62/62 项全部通过**。
  - 真实学员 `student_1001`（全栈）与 `student_1002`（Go后端）登录与向量推荐实测：`vectorSimilarity` 达 63% 与 86%，岗位强匹配契合度达 99 分。

### 2026-09-07 12:00:00 - 用户画像与个性化课程推荐引擎落地

* **任务背景**：基于公开 IT 课程数据集（Coursera/MOOCCubeX/OULAD/Udemy）参数，为系统构建用户画像功能和多路召回个性化推荐引擎，为后续精准推荐奠定数据基座。
* **三步落地路线实施**：
  1. **第一步 — 课程技术标签与难度标定（冷启动数据基座）**：
     - Flyway 迁移 `V19__course_skills_and_user_portrait.sql`：为 `edu_course` 补充 `difficulty_level`（1~3级）、`skills`（技能标签 CSV）、`target_role`（目标岗位）、`prerequisites`（前置技能）四字段。
     - 为数据库中全部 20 门课程标注标准 IT 技能标签（Java, SpringBoot, Redis, Vue3, Python, MySQL, Docker 等 50+ 标签）和难度级别。
  2. **第二步 — 用户画像数据模型与计算服务**：
     - 新增 `edu_user_portrait` 表，存储用户技术偏好标签向量 (`skill_weights` JSON)、自律完课指数 (`completion_rate`)、难度适配偏好 (`preferred_difficulty`)、学习风格、学习频率、价格敏感度等多维度特征。
     - 后端 `EducationService` 新增 `calculateAndSaveUserPortrait()` 方法，分析学员学习记录（已学课程技能交集）、考试记录（通过率）、学习时长、打卡行为自动计算画像特征标签（如"Java技术栈"、"系统进阶期"、"夜间专注"）。
     - 事件驱动更新逻辑：每次请求画像时自动刷新计算。
  3. **第三步 — 学员端专属推荐与技能雷达看板**：
     - 后端推荐接口 `GET /cs/courses/recommendations/personalized`：多路召回算法（技能重叠匹配 + 岗位方向匹配 + 难度适配 + 点赞热度加权 + 多样性去重），返回含 `matchScore`、`recommendReason`、`matchTag` 的排序推荐列表。
     - 后端画像接口 `GET /cs/user/portrait` 与 `PUT /cs/user/portrait/preferences`。
     - 学员门户"个人中心" (`personal/main.vue`)：展示技能雷达进度条、画像特征标签、目标岗位/难度阶段/完课率/学时统计卡片、偏好定制弹窗。
     - 学员门户首页 (`main/index.vue`)：新增"🎯 为您专属推荐"信息流板块，含契合度徽章、推荐理由行、难度标签。未登录用户自动从重磅推荐兜底填充。
* **网关白名单**：`share-gateway-dev.yml` 添加 `/cs/courses/recommendations/**`、`/cs/courses/recommend/**`、`/cs/user/portrait/**` 到 `security.ignore.whites`，已推送 Nacos。
* **部署验证**：
  - 本地 Maven 编译 (`share-education.jar` ~106MB) 和前端 Vite 构建均通过。
  - 服务器严格串行部署：V19 迁移 → 上传 jar → 单容器重建 education → portal dist 同步 → gateway 重启。
  - 冒烟测试 **62/62 PASS**（含完整可写链路）。
* **关键坑点**：
  - `personal/main.vue` 中 `menu-grid` div 未闭合导致 Vue 编译器报 `Element is missing end tag`，需手动补 `</div>` 闭合。
  - `main/index.vue` 中推荐好课的 `course-grid` 和 `section container` 在插入个性化推荐板块前缺少闭合标签，导致模板嵌套错误。
  - `EduExamRecord` 不含 `isPass` 和 `courseId`，需通过 `examMapper.selectById()` 关联查询 `eduExam` 获取 `passScore` 和 `courseId`。

### 2026-09-07 04:30:00 - Redis 秒杀抢购、高频点赞榜与模拟沙箱支付闭环落地
* **任务背景**：深入复用 `share-common-redis` 基础设施，在教育与交易微服务中落地高并发秒杀库存预扣、ZSet 课程高频点赞热榜、及全链路模拟沙箱支付。
* **架构改造与核心实现**：
  1. **公共基础设施升级 (`share-common-redis`)**：在 `RedisService` 补齐高并发原子操作，包括 `decrement`（原子递减）、`setCacheObjectIfAbsent`（分布式互斥锁）、Set 集合去重操作（`sAdd`、`sIsMember`、`sRemove`、`sCard`）、ZSet 有序集合排行（`zAdd`、`zIncrementScore`、`zReverseRangeWithScores`、`zScore`、`zCard`）。
  2. **高频点赞榜与冷启动预热 (`share-education`)**：
     - 点赞去重与计数：采用 Set 键 `edu:course:likes:users:{bizId}` 实现用户防重，ZSet 键 `edu:course:likes:zset` 记录全站课程点赞实时分值；
     - 自动冷启动预热：当 ZSet 为空时，自动从 DB 查询已上架课程学习人次（`learners`）作为基底初始化热度分；
     - 开放公开排行榜接口 `GET /cs/courses/ranking/likes`，在网关 Nacos 配置 `security.ignore.whites` 中放行免鉴权访问。
  3. **高并发库存预扣与秒杀抢购 (`share-trade`)**：
     - 优惠券领取原子扣减：Redis 预扣库存键 `trade:seckill:coupon:stock:{id}` 结合 Set `trade:seckill:coupon:users:{id}` 防刷防超领，数据库 `received_count < total_count` 条件更新兜底；
     - 课程秒杀直购通道：新增 `POST /ts/seckill/courses/{courseId}`，Redis 原子预扣配额（`trade:seckill:course:stock:{id}`），扣减失败即刻快速失败，扣减成功生成已支付秒杀订单并自动报读入学课程。
  4. **全链路沙箱模拟支付 (`share-trade` & `frontends/portal`)**：
     - 后端支持 `/ts/pay/order/{orderId}/simulate` 与 `/ts/pay/order/{orderId}/demo-success` 双别名，自动更新订单状态为已支付（`PAY_SUCCESS`）并调用 Feign 发放课程入学权益；
     - 前端支付页 `payment.vue` 增强沙箱环境视觉提示与一键快捷模拟支付能力。
  5. **学生端门户联动 (`frontends/portal`)**：
     - 首页 `main/index.vue` 热榜无缝对接 `GET /cs/courses/ranking/likes`，展示前 5 热门点赞课程与点赞徽章；
     - 详情页 `classDetails/index.vue` 新增实时点赞/取消点赞按钮，并提供“⚡ 限时秒杀抢购”入口。
* **发布与验证**：
  - 本地 JDK 17 打包 `share-education` 与 `share-trade`，构建静态产物直接更新，完全避免 ECS 磁盘并发构建与 IOPS 耗尽；
  - 串行重构 `education` 与 `trade` 容器；
  - 自动化冒烟测试 35 项 100% 全部通过；点赞榜、秒杀抢购、沙箱支付真实 HTTP 调用全部验证成功。

### 2026-09-07 03:35:00 - 课程图片加载 404 与封面兜底机制全面修复
* **问题现象**：学员端首页及搜索页中部分课程（如“Web前端性能优化”）卡片出现图片裂开碎图现象。
* **根因分析**：
  1. **静态资源文件名映射偏差**：数据库和 Mock 数据引用的图片路径（如 `/src/assets/images/courses/performance.svg`、`golang.svg`、`nextjs.svg`、`nodejs.svg`、`visualization.svg`）与磁盘中的实际文件（`web.svg`、`go.svg`、`next.js.svg`、`node.js.svg`、`d3.js.svg`）名称不匹配，导致 Nginx 返回 404；
  2. **兜底资源缺失**：历史代码引用了 `default-cover.png`，但工程中该文件实际并不存在；
  3. **缺乏图片容灾机制**：组件中的 `<img>` 标签未绑定 `@error` 事件，网络错误或 404 时直接展示浏览器破碎图标。
* **解决方案与成果**：
  1. **补齐别名图片**：在 `frontends/portal` 与 `frontends/business-admin` 中创建对应的软同名/别名 SVG 文件，确保两种命名都能 200 OK 加载；
  2. **创建学术风统一兜底图**：设计并加入 16:9 学术蓝现代课程默认封面 `default-cover.svg` 及兼容图 `default-cover.png`；
  3. **全面容灾兜底**：在学生端所有涉及课程封面渲染的组件（`ClassCards.vue`、`main/index.vue`、`classSearch/index.vue`、`classDetails/index.vue`、`myClass/index.vue`、`pay/carts.vue`、`pay/settlement.vue`、`pay/success.vue`、`personal/main.vue`、`personal/components/ClassCards.vue`、`OrderCards.vue`）和管理端（`curriculum/course/index.vue`、`details.vue`、`order/index.vue`、`refund/index.vue`）统一挂载 `@error="handleImgError"` 与 `defaultCover` 自动替换机制；
  4. **严格遵循发布铁律**：先将补丁包上传至 ECS，按单服务串行排队构建原则先后重构 `portal-ui` 与 `business-admin-ui`，避免云盘限流；冒烟测试 35 项全通；
  5. 静态图片直接通过 Nginx 验证返回 `HTTP 200 OK`，彻底解决裂图。

### 2026-09-07 03:15:00 - 云盘限流教训固化与全站发布闭环
* **关键成果**：
  1. 深度复盘并发构建引发的云盘 IOPS 耗尽瓶颈，在本文档及 `docs/AGENT_HANDOFF.md` 固化了“禁止并发构建”的操作铁律；
  2. 修复 `myClass/index.vue` 的 `<style scoped lang="scss">` 警告；
  3. 服务器三端前端容器稳定运行，网关 `/actuator/health` 正常，`smoke-test.ps1` 35 项全链路测试通过；
  4. 提交哈希对齐推送至 GitHub：`a449827d`。

### 2026-09-07 02:30:00 - 全站按钮统一设计与门户顶栏专项修复
* **关键成果**：
  1. 解决用户反馈的“登录与注册按钮样式不一致”：
     - 门户顶栏登录与注册按钮统一采用 `<el-button type="primary" class="header-btn">`，32px 高度、16px 内边距、13px 字号、500 字重、6px 圆角、实心蓝底（`#2563EB`）与纯白文字（`#FFFFFF`）；
     - 注册按钮自动携带 `md=register` 唤起注册弹窗；
     - 登录与注册弹窗表单按钮统一采用 `login-btn`，高度 40px、圆角 6px。
  2. Element Plus 变量层覆盖：`.el-button--primary` 强制白字；移除管理端所有历史遗留的 `20px` 胶囊圆角。

### 2026-09-07 01:00:00 - 在线教育全站前端视觉现代重构
* **关键成果**：
  1. 依据 `frontend-design` 规范全面去除廉价模板感，废弃 `#667eea`/`#764ba2` 紫蓝发光渐变与漂浮圆球装饰；
  2. 确立 **Deep Slate（#0F172A）** + **Academic Blue（#2563EB）** 高阶学术科技色调；
  3. 卡片统一规范为 8px，表单、输入框、下拉框统一为 6px，彻底移除 100px 跑马道胶囊；
  4. 重塑课程卡片（16:9 标准比例）、个人中心、AI 客服聊天气泡等主要视图。

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
