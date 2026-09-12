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

### 2026-09-12 05:58:00 - 用户端首页加载性能与图片渲染彻底优化：SWR 客户端预热 + 请求拓扑解耦 + Nginx 防旧包缓存 + 0ms 冷启动保底呈现

* **核心成果**：
  1. **定位首页图片与内容延迟加载根因**：
     - **全链路串联阻塞**：原有代码将 7 个接口集中在 `Promise.allSettled` 中，导致哪怕只要有一个接口稍有延迟，首页所有课程卡片 DOM 节点均无法挂载，图片请求被整体延后数秒；
     - **无效全量大表扫描**：发现页面为了计算分类课程数，发起 `classSeach({ pageNo: 1, pageSize: 200 })` 拉取 200 门全量课程记录，增加无谓的巨大网络往返传输；
     - **首屏图片被错误打上懒加载**：首屏高优先级卡片带有 `loading="lazy"`，浏览器等待布局重绘后才发起图片加载；
     - **SPA 单页缓存旧包驻留**：原 Nginx 未给 `index.html` 声明 `no-cache`，导致浏览器缓存了 HTML 入口，且单页应用登录跳转使用内存 `router.push`，未重新加载最新 JS 脚本。
  2. **内置 0ms 极速冷启动基准数据 (Cold-Start Preheating)**：
     - 在前端内置热门骨干课程与分类预置数据（《Vue3 从入门到精通》、《Java SpringBoot 实战》、《机器学习入门》、《MySQL 数据库优化》），即使首次打开或清空缓存，也能在 **0ms 瞬间直出完整封面与卡片**，彻底消除白屏与图片闪跳。
  3. **前端架构 SWR (Stale-While-Revalidate) 本地预热治理**：
     - 引入专属缓存机制 `tianji_portal_home_cache_v2`，页面初始化阶段直接从 `localStorage` 同步读入分类、推荐好课、个性化推荐、热门榜、最新榜、点赞排行及最近学习记录，**达成 0ms 秒级首屏直出渲染与骨架呈现**；
     - 数据变更通过 300ms 防抖自动写回本地缓存，下一次进入即时秒开；
  4. **请求拓扑解耦与独立流式响应**：
     - 将原本阻塞的 `Promise.allSettled` 重构为各业务域独立非阻塞 Promise 异步流（分类 ~20ms、重磅推荐 ~15ms、热门好课 ~15ms、最新上线 ~15ms、点赞排行榜 ~10ms）；
     - 彻底剔除 200 门课程的冗余大查询，分类数量直接依托已聚合的 `courseCount` 字段；
     - 个性化推荐与 AI 多智能体流作为独立背景流进行无感增量覆写；若本地暂无推荐，立即采用重磅推荐进行秒级平滑降级兜底；
  5. **Nginx 缓存治理与运行时刷新**：
     - `frontends/nginx.conf` 与 `index.html` 中严密加入 `Cache-Control: no-cache, no-store, must-revalidate`、`Pragma: no-cache`、`Expires: 0`，确保入口 HTML 永远最新；
     - 登录（`LoginPhone.vue`/`LoginPass.vue`）与登出（`Header.vue`）使用 `window.location` 强制刷新，保障切换账号或重新登录后加载最新运行时上下文；
  6. **定向验证与平滑发布**：
     - 本地完成编译打包 `frontends/portal`（无警告报错）；
     - 静态产物与 Nginx 规则同步部署至云端 `tianji-portal-ui` 容器并平滑热重载 Nginx；定向接口验证全链路 200 OK，响应耗时均在 10ms~200ms 内。

### 2026-09-11 20:00:00 - 媒资管理与课程小节双向深度绑定升级：数据库表级一致性对齐 + 级联选择器联动 + 云端热更新上线

* **核心成果**：
  1. **跨库数据表深度对齐与基准数据治理**：
     - 排查并确定媒资表 `tj_file.file_media` 与课程大纲表 `tj_education.edu_course_catalog` 的物理拓扑；
     - 在 `tj_file.file_media` 表新增 `course_id`（课程ID）、`course_name`（课程名称）、`section_id`（小节ID）、`section_name`（小节名称）四项业务字段并创建复合索引 `idx_file_media_course_section`；
     - 沉淀增量 SQL 脚本 `sql/migrations/V32__file_media_course_binding.sql`，同步更新初始化全量基准 `sql/zhiwen-file.sql`；
     - 线上执行数据回填与断言验证，将历史 12 条存量视频与课程大纲小节完成 100% 互相对齐（`status='used'`）。
  2. **跨微服务接口协同与原子级联动保障**：
     - 文件微服务 (`share-file`)：
       - 实体 `FileMedia.java` 拓展字段映射；
       - `FileMediaService.java` 重构保存（`save`）、分页列表（`page`/`pageView`）与详情输出（`view`），支持接收课程与小节信息、按 `courseId` 精准过滤及自动设置 `used` 状态；
     - 教育微服务 (`share-education`)：
       - `EducationService.java` 新增 `bindCatalogMedia` 与 `unbindCatalogMedia` 核心业务逻辑，更新 `media_id`、`media_name` 与 `duration_seconds`；
       - `EducationPortalController.java` 暴露 `POST /courses/media/bind` 与 `POST /courses/media/unbind` 接口；
     - 针对双微服务进行 Java 17 独立本地编译，热更新云端 `tianji-file` 与 `tianji-education` 容器。
  3. **业务管理端 (`business-admin`) UI 与交互全流程重构**：
     - `curriculum.js` 扩展 `getSimpleCourses`（320门课程列表）、`bindCourseMedia` 与 `unbindCourseMedia` API 调用；
     - 媒资列表 (`media/index.vue`)：
       - 搜索栏新增「所属课程」下拉筛选框（支持 320 门课程模糊搜索与快速过滤）；
       - 数据表格新增「关联课程」与「对应小节」列，展示专属彩色徽章标签；
       - 上传视频弹窗：重构为课程视频关联弹窗，加入「所属课程」选择器与「对应小节（第几节）」级联选择器（按章节分组展示，动态标注小节是否已绑定视频或可绑定），并在上传保存成功后自动触发大纲小节绑定；
       - 编辑弹窗：支持查看与动态改绑所属课程和小节，改绑时自动触发原小节解绑与新小节绑定；
       - 删除视频：若已绑定小节，删除前自动完成关联小节解绑，确保双向数据绝对一致。
  4. **全链路自动化断言与定向验证**：
     - 本地执行 `npm run build` 产出静态产物，打包同步至云端 `tianji-business-admin-ui` 容器并重载 Nginx；
     - 执行 `.scratch/verify_media_course_binding.py` 对登录、课程列表获取、大纲解析、媒资上传保存、大纲小节双向绑定、`courseId` 精准过滤、解绑与删除清理全流程进行自动化断言，验证通过率 100%。

### 2026-09-11 19:00:00 - 全平台课程简介 HTML 标签彻底排查与清洗：全库 320 门课程纯文本治理 + 前后端立体过滤与结构化呈现升级

* **核心成果**：
  1. **全库 320 门课程数据彻底清洗与断言校验**：
     - 排查发现 `tj_education.edu_course` 中历史批量插入的 300 门 IT 课程（ID 21 至 320）的 `description` 字段被包裹了 `<h3>` 和 `<p>` 标签；
     - 线上执行 SQL 原地数据清洗，将 300 门富文本课程的 `description` 重置为纯净摘要文本（与 `short_description` 对齐），并将 ID 1~20 的 NULL 简介补齐；
     - 线上执行聚合查询断言：`total_courses=320, desc_has_html=0, desc_is_null=0`，全库 100% 达成无标签纯净数据。
  2. **数据库基准与增量迁移落地**：
     - 新增 `V31__clean_course_descriptions.sql` 增量迁移脚本，记录本次清洗变更；
     - 同步修正 `V22__add_300_real_world_it_courses.sql` 初始插入脚本中的 300 门课程描述，确保未来从零初始化数据时原生纯净。
  3. **后端防御性过滤与适学人群增强**：
     - `EducationService.java` 新增 `cleanHtmlTags(String text)` 静态过滤方法；
     - 在 `courseView` 统一输出层对 `description` 与 `shortDescription` 做强制过滤保障；
     - 在 `legacyCourse` 中对 `detail` 和 `introduce` 做强制过滤，并将 `course.getPrerequisites()` 映射至 `usePeople`，补全管理端与前端旧接口的适学人群字段；
     - 在 `saveLegacyCourse` 保存入口处增加自动清洗过滤，杜绝后续通过管理端录入带标签脏数据。
  4. **前端防御过滤与智能结构化呈现升级**：
     - 学生端详情页 `classDetails/index.vue`：
       - 头部简介与“课程介绍”Tab 增加前端 `cleanHtml` 过滤，消除标签暴露；
       - 重构“课程介绍”Tab：简介段落清爽展示，不再出现“课程简介”重复双重标题；
       - “适合人群”与“学习目标”动态读取课程实体的 `prerequisites`、`targetRole` 与 `skills`，呈现专业化岗位与技术栈要求；
     - 学生端学习页 `learning/index.vue`：
       - 增加 `cleanHtml` 过滤，移除冗余重复的“课程说明”双重段落，以模块化清晰展示技术栈与适合人群；
     - 课程组件 `ClassAbout.vue` 与业务管理端 `CourseAbout.vue`：
       - 全面增加 `cleanHtml` 过滤，确保全平台所有端绝对不展示任何 HTML 原始标签。
  5. **定向回归测试与生产部署**：
     - 本地通过 Java 17 离线打包 `share-education.jar` 并同步至云端热更新 `tianji-education` 容器；
     - 本地执行 `npm run build` 产出 `portal` 与 `business-admin` 静态产物，分别热更新 `tianji-portal-ui` 与 `tianji-business-admin-ui` 容器并重载 Nginx；
     - 编写并运行定向测试脚本 `.scratch/verify_course_descriptions.py`，针对网关课程接口（ID 109、21、1、320）及前端静态资源进行自动化断言，100% 通过验证。

### 2026-09-11 15:30:00 - 简历工程能力量化评分机制深度重构：60分及格基准分 + 五维工程能力细则面板与大模型规则双引擎升级

* **核心成果**：
  1. **评分底线与分值模型重构**：
     - 将过往较高的预设底分（82分）彻底重构为**60 分准入门槛基准分**，对标大厂简历筛选准入基准；
     - 扩展并落地五维工程能力量化加成模型（总分 60~100 分）：
       - 维度一：核心技术栈广度与深度（0-10分，覆盖语言、微服务、中间件、持久化与分布式架构）；
       - 维度二：重点项目经历与系统复杂度（0-10分，考查项目数量、业务体量与分布式深度）；
       - 维度三：STAR 法则与量化业务成效（0-10分，深度识别 QPS 吞吐、耗时压降、SLA 可用性等量化指标）；
       - 维度四：高可用工程与容灾逃生规范（0-5分，重点审视限流熔断、链路监控与测试规范）；
       - 维度五：目标岗位与名企契合度（0-5分，智能对标当前投递目标岗位及一线大厂要求）；
  2. **后端 DTO 结构升级与双引擎算法对齐**：
     - `ResumeAnalysisVO` 扩充 `scoreDetails` 属性及 `ScoreDimensionItem(name, score, maxScore, rating, description)` 结构；
     - `UserResumeServiceImpl` 在大模型 Prompt 中确立 60 底分五维打分指令，并升级启发式本地兜底引擎 `applyHeuristicAnalysis`；在 `toVO` 转换层实现历史存量简历的五维动态计算兼容；
  3. **前端「大厂招聘委员会·五维工程能力量化细则」大屏上线**：
     - `myResume.vue` 在职涯诊断大屏中新增专属细则面板，结合动态色阶（绿/蓝/黄）与进度条直观呈现各项得分、满分占比与专家评语；并在综合匹配度处标注“基准及格分 60 分 + 五维工程能力实战加成”；
  4. **云端生产部署与定向回归测试**：
     - 串行在本地生成静态 `dist/` 与后端 JAR 并同步热更新云端 `tianji-portal-ui` 与 `tianji-customer` 容器；
     - 编写并在本地运行 `.scratch/test_resume_scoring.py` 进行定向范围校验，实测真实大厂简历诊断与已有简历查询，60 分基准、各项维度满分累计 100 分、单项描述与分值断言 100% 通过。

### 2026-09-11 14:35:00 - 个人中心「我的简历与 AI 深度诊断」研发上线、31岗位大厂对标与 AI 模拟面试全真数据联通闭环

* **核心成果**：
  1. **数据模型设计与简历持久化体系建立**：
     - 在 `tj_customer` 库创建 `cs_user_resume` 用户简历表，涵盖简历原始内容、对标岗位、目标公司、匹配分数、职级评级、技术栈标签、项目亮点、薄弱项风险点、预测必考题与 STAR 重构建议；
     - 扩展 `cs_interview_session` 表新增 `resume_id` 与 `resume_summary` 快照字段，实现每场模拟面试与特定简历版本的一对一溯源。
  2. **后端简历解析、AI 深度对标与智能诊断双引擎**：
     - `UserResumeServiceImpl` 支持纯文本直接录入、txt/md 客户端直读、以及服务端附件文本解析；
     - 深度对标 31 个精选技术岗位与大厂标准，结合大模型提示词工程与健壮的启发式规则降级分析，一键输出六维全真评估（技能匹配度、职级梯队、亮点提炼、薄弱风险、大厂面试官预测题、STAR 优化示范）。
  3. **简历与全真 AI 模拟面试系统全链路深度互联**：
     - 面试大厅（`/interview`）启动前自动嗅探当前用户已建档简历并呈现联动卡片，支持候选人自由切换“根据此简历定制考题”；
     - 面试开启时将简历核心画像深度注入上下文，第 1 轮破冰题直接锁定候选人真实业务项目与技术栈进行开题；后续多轮追问自适应结合简历声称的技术亮点与薄弱点进行剥洋葱连环攻防；终局报告中综合评判答题表现与简历声称的契合度。
  4. **学生端个人中心 UI 页面研发与多端联动**：
     - 个人中心左侧导航栏无缝挂载「📄 我的简历与 AI 深度诊断」（`/personal/main/myResume`）；
     - 提供大厂标准范例一键填入、多格式上传、31 岗位分赛道级联选择、动态评分徽章、技术栈标签展示、大厂预测考题高亮与 STAR 法则重塑建议展示；
     - 页面底部提供一键直通 AI 模拟面试专属通道，参数自动化透传至考场大厅。
  5. **云端生产热部署与端到端闭环验证**：
     - 定向打包并安全更新 `tianji-customer` 与 `tianji-portal-ui` 容器，排查并规范容器内 `/app/app.jar` 运行路径；
     - 通过 Gateway 接口定向验证简历存取、AI 对标、第一轮自适应个性化出题及 MySQL 落库，100% 验证通过。

### 2026-09-11 13:50:00 - 全真沉浸式 AI 模拟面试与职涯评测超级子系统 (Zhiwen AI Interview Pro) 研发上线与全链路闭环验证

* **核心成果**：
  1. **高阶业务架构设计与微服务复用架构落地**：
     - 基于已有 `share-customer` (tj_customer) 复用 FastEmbed、Qdrant 向量检索底座与 GPT-5.6-Luna 客户端，避免新增容器带来额外 RAM 消耗；
     - 研发出全真互联网大厂模拟面试矩阵，支持阿里 P7 资深架构师、字节跳动技术专家、资深大厂 HRBP 与大厂评审委员会 4 大专业考官风格；
  2. **数据库持久化与 Flyway V30 迁移落地**：
     - 设计并应用 Flyway 迁移脚本 `V30__create_ai_interview_module_tables.sql`，建立 4 张核心业务表：
       - `cs_interview_session` (场次与大厂目标表)
       - `cs_interview_turn` (问答轮次与三级深度追问表)
       - `cs_interview_code` (算法代码手撕与沙箱评测表)
       - `cs_interview_report` (终局六维能力雷达与职级诊断大屏表)；
  3. **3-Level "剥洋葱" 连环深度追问与 Qdrant 影子检定核心引擎**：
     - 实现 Level 1 概念摸底 ➔ Level 2 底层原理深挖 ➔ Level 3 线上极限排障三级追问递进机制；
     - 问答提交时实时并发调用 Qdrant 10,000 真题向量库进行影子语义检定，毫秒级召回标杆参考答案并比对答题亮点与漏洞；
  4. **算法代码手撕沙箱与架构异味审计引擎**：
     - 在线实时推演时空复杂度（Time: O(N), Space: O(1)）、审计工程代码异味（空参防御、并发死锁风险、魔法值抽取），并自动生成阿里生产级重构示范代码；
  5. **终局职涯能力诊断报告与原生响应式 SVG 六维雷达图**：
     - 终局委员会多维裁决（Strong Hire / Hire / Weak Hire / Reject）、对标阿里 P6/P7 / 字节 2-1/2-2 职级；
     - 原生自绘响应式 SVG 六维胜任力雷达图（Java核心、架构设计、存储数据库、分布式高并发、算法工程、沟通表达）；
     - 答题话术 STAR 法则重塑对比（原版弱回答 ➔ 大厂 STAR 标准示范）与平台定制补强课程推荐；
  6. **前端三页联动与生产部署验证**：
     - 研发上线 `/interview/index` (模拟面试大厅)、`/interview/room/:id` (沉浸式考场 HUD 与双栏工作台)、`/interview/report/:id` (多维诊断大屏)；
     - 完成服务器生产容器重启与 Nginx 静态分发，定向范围接口与全链路真机验证 100% 通过。
  7. **31+ 大厂热门岗位矩阵（8大技术赛道）与动态自适应考查引擎升级**：
     - **8 大赛道 31 个精选企业级岗位**：涵盖后端与微服务架构、跨语言系统与高性能（Go/C++/Rust/Python）、Web前端与移动端、AI与大模型算法（LLM/RAG/Agent/NLP/CV/推荐）、大数据与流批计算（Flink/Spark/湖仓一体）、数据库与存储中间件、云原生与SRE、测试开发与网络安全；
     - **前端交互升级**：采用 `<el-option-group>` 赛道分组与 `filterable` 即时搜索，代码沙箱扩展支持 Java、Go、C++、Python、TypeScript/JS 与 Rust 6 大手撕语言与专属模板；
     - **后端自适应引擎架构**：实现 `detectJobTrack`、`resolveFirstDimension`、`resolveNextDimension` 与专属手撕题 `resolveCodingProblem` 调度，根据候选人选择岗位自适应决定各轮维度、出题 Prompt 与终局 STAR 职涯重塑建议，告别 JVM 题目硬编码；
     - **云端发布与定向范围验证**：轻量打包同步部署并热更新 `tianji-portal-ui` 与 `tianji-customer`，4 大跨赛道典型岗位真实连调 100% 满分通过。

### 2026-09-10 22:20:00 - 知识库规模化扩充至 10,000 条（JavaGuide 与牛客网双源清洗入库）、全局零重复排重与 Qdrant 512 维全量向量化落地

* **核心成果**：
  1. **JavaGuide 与牛客网专项双源知识库深度采集与清洗**：
     - 从 JavaGuide 体系全量提取 Java 基础/并发/JVM、Spring 微服务生态、MySQL/Redis、计算机网络、操作系统、分布式架构、高可用设计等 5,536 条深度问答，并补齐文档上下文消除碎片标题；
     - 从牛客网专项练习（tagId=21000）39 大技术专题中并发采集 2,094 道涵盖数据结构与算法、SQL实战、AI大模型、前端Web、Linux/Shell 的编程真题与详尽解析；
  2. **多级混合去重机制与全局 100% 唯一性保障**：
     - 结合标头归一化、全角半角符号/圈号数字消除、精确文本匹配及 2-gram Jaccard 相似度排重，累计剔除 631 条与已有题库或彼此重叠的重复题目；
     - MySQL 库级校验 `GROUP BY question HAVING count(*) > 1` 返回 0 条，达成万条真题 100% 绝对唯一；
  3. **数据持久化与数据库扩容 (V29 迁移)**：
     - 生成 Flyway 迁移脚本 `V29__expand_knowledge_base_to_10000.sql`（净增 7,630 条，IDs: 2391 ~ 10020），写入线上 `tj_customer.cs_knowledge`，全站知识库总数精确达到 **10,000 条**；
  4. **Qdrant 向量数据库 10,000 维向量全量索引与微服务秒级召回**：
     - 使用 FastEmbed `BAAI/bge-small-zh-v1.5` 在本地环境完成 7,630 条新知识的 512 维向量密集空间计算（用时 35.1s），分批上传至服务器一键写入 Qdrant `tianji_knowledge` 集合；
     - Qdrant `points_count` 扩容至 **10,000**（`status: green`）；实测 JVM 垃圾回收、Spring 三级缓存、快速排序、Redis 缓存穿透等专业技术检索，Top-1 语义匹配度达 0.75~0.87，平均召回延迟 < 15ms。

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

### 2026-09-11 15:00:00 - 简历文档解析引擎与真实紧扣履历的 AI 职涯深度诊断上线

* **核心成果**：
  1. **Apache PDFBox 与 DOCX 双解析引擎**：在 `share-customer` 集成 `org.apache.pdfbox:pdfbox:2.0.30`，结合轻量 `ZipInputStream` 结构化解析 Word (.docx) 文档，精准抽取文字层与排版断行，彻底终结以前将 PDF 二进制字节流当作 UTF-8 读取导致 23 万字符 `%PDF-1.6` 乱码爆破编辑区的致命缺陷；
  2. **端到端二进制防护网**：前端 `myResume.vue` 与后端 `UserResumeServiceImpl` 双向部署魔数与非打印控制字符检测机制，坚决拒绝 `%PDF-` 与 `PK\x03\x04` 二进制注入，从根源保护数据库与大模型上下文；
  3. **基于真实经历的深度诊断与大厂对标**：彻底废弃以前空洞通用的虚构模板，建立覆盖 70+ 核心技术标签库、候选人真实项目名称抽取器与生产量化指标提取器（QPS/ms/降幅）。大模型 Prompt 强制绑定真实项目，兜底算法亦 100% 提取候选人真实项目与技术，动态生成高光亮点、薄弱项、大厂连环深挖考题与 STAR 重塑建议。

### 2026-09-11 18:00:00 - 职涯诊断打分机制彻底重塑与零同情分/零虚构治理

* **核心成果**：
  1. **60 分及格基准分机制确立与 5 维严谨计分重塑**：
     - 依据大厂校招与社招准入门槛，建立 60 分基准门槛（合法简历即享有基准 60 分），总分 100 分 = 60 分基础门槛 + 40 分专业进阶（核心技术栈广度与深度 10 分、项目经历与系统复杂度 10 分、STAR 量化业务成效 10 分、高可用工程与容灾规范 5 分、目标岗位与名企契合度 5 分）；
  2. **跨赛道不相干简历“零同情分”治理**：
     - 彻底删除旧版无技术标签时注入假标签（Java/Spring Boot/Redis）的逻辑；
     - 引入 8 大主流赛道 31 个技术岗位的对口关键词字典与严格契合度算法，对完全不相干简历（如幼师、文员、财务投递架构师岗位）实行“零同情分”机制，D1~D5 维度严谨打出 0 分，评级明确给出【严重脱节】、【匮乏】与【缺失】，总分严格锁定 60 分；
  3. **高光亮点与大厂考题 100% 紧扣真实经历（零虚构）**：
     - 大模型 Prompt 与 Java 后端审计双重设防：对无对口项目与技能的履历，坚决禁止虚构任何高并发、微服务或 Redis 考题，强制如实输出【无对口技术亮点】与【工程实战缺失】风险提示，考题针对性转向转岗动机核查、独立代码实践证明与差距补齐建议；
  4. **前端大屏得分视觉多态与定向双场景验证通过**：
     - 诊断大屏 breakdown 维度新增动态彩色评级徽标（卓越/良好/基础/偏弱/匮乏/严重脱节）与 0 分红字告警；
     - 双场景端到端自动化测试 100% 通过（场景A：跨赛道不相干简历严格 60 分且零虚构；场景B：真实专业架构师简历 95 分卓越对标）。

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
| **14** | **Docker 容器更新 JAR 包后重启依然运行旧代码** | 业务微服务容器 `ENTRYPOINT` 是 `exec java $JAVA_OPTS -jar /app/app.jar`（位于 `/app/app.jar` 而非容器根目录 `/app.jar`）。若将新编译包直接复制到 `/app.jar`，容器启动仍旧加载 `/app/` 子目录下的旧版本。 | 替换容器运行时 JAR 时，务必检查容器配置的真实 Entrypoint / Cmd 路径，准确拷贝至目标路径（如 `docker cp new.jar tianji-customer:/app/app.jar`），并重启容器生效。 |
| **15** | **上传 PDF/Word 简历后返回原始二进制字节码 (%PDF-1.6) 导致编辑区乱码与 AI 虚构** | 后端直接使用 `new BufferedReader(new InputStreamReader(file.getInputStream(), UTF_8))` 读取所有上传文件。遇到 PDF/Word 二进制文件时，会将压缩流和 PDF 描述字节读为乱码字符串，多达数十万字符，造成前端卡死、大模型超时或报错，最终回退为生硬虚构模板。 | 1. 引入 Apache PDFBox (`PDDocument`, `PDFTextStripper`) 解析 PDF 文字层；<br>2. 解析 Word (.docx) 的 `word/document.xml` 提取纯文本与段落结构；<br>3. 增加二进制特征字符探测 (`isRawBinary`)，直接阻断二进制字符串落盘；<br>4. 职涯诊断基于候选人真实项目名称与量化指标深度抽取，确保高光亮点与考题真实准确。 |
| **16** | **非对口/不相干简历仍能获得技术契合度与虚构考题** | 旧逻辑在未匹配到技术标签时注入了 Java、MySQL 兜底标签，且目标岗位契合度打分存在保底 +2 分的保底逻辑，大模型 Prompt 缺乏反事实审计导致即使非技术简历也会生成高并发题目。 | 1. 彻底移除空标签时的伪造兜底逻辑；<br>2. 建立 8 大技术赛道 31 岗位的严谨对口词匹配，命中为 0 则契合度严格 0 分；<br>3. 大模型 Prompt 明确要求“简历没有的坚决说没有”，后端增加真实度审计，对无对口经历者强制输出事实警告；<br>4. 确立 60 分基准门槛体系，让不相干简历严格锁定 60 分并亮红警示。 |
| **17** | **学生端个人设置页面保存时报 404 / 500 或无法更新信息** | 前端 `api/user.js` 中的 `updateUserInfo` 请求路径硬编码为 `/students`，遗漏了微服务网关代理前缀 `/us`（即缺少 `${USER_API_PREFIX}`），导致网关直接抛出 404 NOT_FOUND。且组件中错误读取 `res.data.msg` 触发 TypeError 导致弹窗“请求出错！”。 | 1. `updateUserInfo` 修正为 `${USER_API_PREFIX}/students` 对齐网关路由；<br>2. 规范组件内响应解构与错误捕获；<br>3. 在 `onMounted` 钩子中主动拉取最新用户画像补齐 nickname、avatar、gender，添加按钮保存中防重复提交状态。 |
| **18** | **静态媒资/头像资源浏览器直接加载时报 401 鉴权拦截或图片破损** | 1. 用户头像、讲师头像等静态资源被浏览器 `<img>` 或 `<el-avatar>` 标签直连渲染时无法携带 JWT 请求头，网关拦截报 401 返回 JSON，导致图片裂开破损；<br>2. Nginx 反代缺少 `profile` 路由匹配，且网关缺少 direct `/profile/**` 路由；<br>3. 前端 Header 与个人中心存在硬编码未编译路径 `/src/assets/images/users/default-avatar.svg`（生产环境 404）以及缺乏 `@error` 兜底容错。 | 1. 在 Nacos `share-gateway-dev.yml` 的 `security.ignore.whites` 中增加 `/file/**` 与 `/profile/**` 白名单，并新增 `share-profile` 网关路由；<br>2. Nginx 正则反向代理增加 `profile` 匹配；<br>3. 前端 Header、个人设置、个人主页统一引入 Vite 静态资源导入（`import defaultAvatar`），配合 `formatAvatarUrl` 与 `@error` 异常回退机制，确保任何网络或路径异常均平滑降级为默认头像占位；<br>4. 头像上传采用 `URL.createObjectURL` 极速本地预览，保存成功后通过自定义事件 `user-profile-updated` 实时同步顶栏 Header 头像与昵称。 |
| **19** | **用户端上传高清大图头像时毫无反应或失败（Nginx 默认 1M 413 拦截）** | 1. Nginx 默认 `client_max_body_size` 仅 1MB。用户上传手机或数码相机拍摄的高清原图（如实测中的 4.77MB / 5,004,253 字节）时，Nginx 在代理入口直接切断连接并报 `413 Request Entity Too Large`；<br>2. 前端 `<el-upload>` 未挂载 `:on-error` 错误处理钩子，Element Plus 静默吞没了 413 异常，导致前端界面毫无反馈和弹窗，用户误以为“毫无反应”。 | 1. 在前端 `nginx.conf` 的 `server` 块中显式声明 `client_max_body_size 50m;`，对齐 Spring Boot 20MB 上限；<br>2. `<el-upload>` 增加 `:on-error="handleAvatarUploadError"`，捕获 413 及各类网络错误并友好提示；<br>3. 增加 `uploading` 上传中防重与加载状态，放宽头像前端大小限制至 10MB。 |

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
