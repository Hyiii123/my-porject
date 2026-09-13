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
| **推荐算法微服务** | `http://47.120.67.187:15000` | 容器 `tianji-recommend`，内部端口 `5000`，`DRAG-KP4SR` 语义桥接引擎 |
| **Nacos 控制台** | 内部端口 `8848` / 宿主机 `8848` | 配置中心与服务发现（命名空间等依赖外部 MySQL） |

---

### 三、重大里程碑记录指引 (Milestones Decoupled Notice)

> ⚠️ **重要规范变更通知**：
> 为保证工作日志与运维手册的聚焦性与轻量化，自 2026-09-12 起，**重大里程碑记录已彻底剥离至专属文件**。
> [`AGENT_WORKLOG.md`](file:///D:/education%20system/my-porject/AGENT_WORKLOG.md) 今后**不再记录任何里程碑内容**。所有真实工程交付物、重大业务功能上线、底层架构演进与技术突破，**一律且唯一记录在：**
> 👉 **[`AGENT_MILESTONES.md`](file:///D:/education%20system/my-porject/AGENT_MILESTONES.md)**
>
> 严禁将日常例行操作（如启停服务、基础排查）混入里程碑。请各位智能体严格遵循。

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
| **20** | **短信登录改版为邮箱验证码登录时旧有接口协议与鉴权模式不兼容** | 1. 用户端原短信登录走 `/accounts/login` 传 `cellPhone` 与 `code`，直接修改可能导致旧版接口报 500 或用户名格式校验拒绝；<br>2. 新用户直接使用邮箱验证码登录若未提前注册，传统模式会直接报用户不存在导致登录失败；<br>3. 邮箱验证码未设置防重放机制可能被复用。 | 1. 在 `LoginBody` 拓展 `email`、`code`、`type`，并做向后兼容；<br>2. 鉴权服务 `SysLoginService` 中新增 `loginByEmailCode`，校验 Redis 验证码后原子销毁（防重放）；<br>3. 自动检测用户是否存在，未注册用户自动为其创建学员账号（`user_type: "01"`）并直接发牌（JWT），达成免密极速登录体验；<br>4. 前端 `LoginPhone.vue` 转型为专用 QQ 邮箱验证码登录组件，内置 60s 倒计时与严格邮箱正则校验。 |
| **21** | **用户端首页继续学习横幅对新用户错误显示「上次学习数据可视化」虚假记录** | 1. 首页 SWR 缓存机制将属于用户私密态的 `recentLearning` 与全站公开课程分类缓存混存于 `localStorage` 的 `HOME_CACHE_KEY`，导致换账号或新用户继承老用户的脏缓存；<br>2. `getMylessons()` 在返回空列表（新用户）时缺少 `else` 重置分支，导致界面永久保留脏缓存；<br>3. 模板字段与后端实体字段不一致（`learnedSections` vs `completedLessons`、`sections` vs `totalLessons`），导致进度计算错误回退为 0/10 节。 | 1. `recentLearning` 彻底从公开 `HOME_CACHE_KEY` 解耦，初始化与无记录时严格为 `null`，并在读取缓存时主动清理历史存量键；<br>2. `loadRecentLearning` 严格判断 `lessons.length > 0`，无记录时强制重置为 `null`；<br>3. 兼容后端真实返回字段（`completedLessons`、`totalLessons`、`progressPercent`），确保真实学员进度精准无误。 |
| **22** | **个人中心点击「我的课程」跳转 404 页面** | Vue Router 4 中存在路由名称冲突：`components.js` 的独立顶层课表路由 `/my-class` 也声明了 `name: 'myClass'`，后注册的顶层路由覆盖并注销了 `base.js` 中个人中心子路由 `/personal/main/myClass` 的 matcher。同时 `/:w+` 404 兜底路由原本写在 `defaultRouterList` 头部，导致匹配失效直接被转派至 `/result/404`。 | 1. 将 `components.js` 顶层课表路由重命名为 `name: 'myClassSchedule'`，彻底消除路由名冲突；<br>2. 规范修正 `base.js` 与 `components.js` 内部父子路由同名隐患（`searchIndex`, `detailsIndex`, `learningIndex` 等）；<br>3. 将 `notFoundRouter` (`/:w+`) 规范移至全部路由列表的最末尾（`[...defaultRouterList, ...asyncRouterList, ...notFoundRouter]`）；<br>4. `myClass.vue` 增加加载骨架、空状态提示与接口容灾，彻底杜绝无课程时的异常弹窗与白屏。 |
| **23** | **容器间 HTTP 调用报 Connection Refused 或解析失败** | 同一 Docker 网络内，容器之间通信必须使用 Docker 服务名/容器名（如 `http://tianji-recommend:5000`），若误写为 `127.0.0.1:5000` 则请求打向容器自身，导致连接被拒。 | 在 Nacos 动态配置及微服务调用地址中，容器互联严格配置为 Docker 服务名域名（如 `http://tianji-recommend:5000/api/recommend/predict`），严禁在容器内使用 `127.0.0.1` 指向同宿主机其他容器。 |
| **24** | **第三方大模型接口网络抖动或不可达导致智能体推荐接口长久挂起阻塞** | 智能体在调用 DashScope/OpenAI 兼容接口时若遇到代理不可达或长耗时，多次重试叠加后单个推荐请求可被阻塞长达 2 分钟以上，耗尽 Servlet 线程池。 | 在 `DashScopeAiClient` 等 AI 客户端中引入原子熔断器（Circuit Breaker）机制，当连续超时或网络中断时立即熔断并保持熔断窗口（如 5 分钟），请求瞬间无感回退至确定性知识图谱规则生成逻辑（耗时 < 1ms），保障系统高吞吐与高可用。 |
| **25** | **MyBatis-Plus 与 Spring Boot 3.2 / Spring 6.1 `factoryBeanObjectType` 及双 JAR 混淆** | 1. 旧版 `mybatis-plus-boot-starter:3.5.3.1` 依赖 `mybatis-spring:2.1.2`，在 Spring 6.1 下会将 `factoryBeanObjectType` 设为 String 导致启动异常；<br>2. 传递依赖同时拉入 3.5.3.1 和 3.5.6 两个不同版本的 MyBatis-Plus Starter，导致老版本的 `ddlApplicationRunner` 返回 `null` 触发 Spring Boot 3.2 的 `NullBean` Runner 异常。 | 1. 采用专门适配 Spring Boot 3 的 `com.baomidou:mybatis-plus-spring-boot3-starter:3.5.6`（内置 `mybatis-spring:3.0.3`）；<br>2. 在服务的 pom.xml 中对 `share-common-redis` 和 `share-common-log` 等模块显式 `<exclusions>` 排除旧版 `mybatis-plus-boot-starter`，彻底消除双 JAR 混淆；<br>3. 在主类中兜底注入 `ddlApplicationRunner` Bean，确保容器在任何环境下均稳定运行。 |
| **26** | **Spring AI Starter 自动装配在空 API Key 下抛异常导致微服务闪退** | 官方 `spring-ai-openai-spring-boot-starter` 内置的 `OpenAiAutoConfiguration` 会在初始化时对配置的 API Key 执行 `Assert.hasText(apiKey)` 严格非空断言。若环境变量未配 Key（或为空字符串），容器将直接抛出 `IllegalArgumentException: OpenAI API key must be set` 崩溃。 | 1. 在微服务主类上显式排除该自动装配类：`@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})`；<br>2. 在自研 `SpringAiConfiguration` 中声明具有安全兜底值（`dummy-key-for-spring-ai-init`）的标准 `OpenAiApi`、`OpenAiChatModel` 与 `ChatClient`；<br>3. 配合 `DashScopeAiClient` 的毫秒级原子熔断器与规则降级引擎，实现“零配置也不崩、配置后自动启用、故障时秒级降级”的企业级高可用。 |
| **27** | **Spring Boot 3.2 与 Spring Cloud 2022 版本兼容性验证拦截启动** | 在实施定向升级策略（单个领域微服务升级到 Spring Boot 3.2.4，其余服务保持 Spring Cloud 2022.0.5）时，Spring Cloud 内置的版本兼容性校验器 `compatibility-verifier` 会因非 3.0.x/3.1.x 而主动阻断引导上下文。 | 在微服务 `bootstrap.yml` 中声明 `spring.cloud.compatibility-verifier.enabled: false`，安全关闭引导期版本断言，实现跨大版本的领域微服务平滑共存。 |

---

## 五、每次工作执行清单 (Standard Operating Procedure)

```mermaid
flowchart TD
    A[收到用户需求] --> B[阅读 AGENT_WORKLOG.md, AGENT_MILESTONES.md & docs/AGENT_HANDOFF.md]
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
    L --> M[更新 AGENT_WORKLOG / AGENT_MILESTONES 并向用户交付]
```

### 1. 动工前检查 (Pre-flight)
- [ ] 阅读本 `AGENT_WORKLOG.md` 确认最新的卡点与注意事项。
- [ ] 阅读 [`AGENT_MILESTONES.md`](file:///D:/education%20system/my-porject/AGENT_MILESTONES.md) 了解最新业务演进与里程碑。
- [ ] 检查本地 Git 状态：`git status`、`git branch`、`git log -3 --oneline`。
- [ ] 检查服务器运行状态：`docker compose -p tianji-share ps`、网关健康 `curl http://127.0.0.1:8080/actuator/health`。

### 2. 变更中约束 (In-flight)
- [ ] 仅修改与本次任务直接相关的文件，绝不扩大战线。
- [ ] 如需更新多个服务，必须串行排队构建，禁止并发多构建。

### 3. 验收与归档 (Post-flight)
- [ ] 针对改动范围运行定向验证或冒烟测试。
- [ ] 代码拉回本地后，执行 `git diff --check`。
- [ ] 提交信息使用规范语义（`feat:`, `fix:`, `style:`, `docs:`, `refactor:`）。
- [ ] `git push origin master`，并核对 `git rev-parse HEAD` 与 `git ls-remote`。
- [ ] 若有重大里程碑或技术突破，记录至 [`AGENT_MILESTONES.md`](file:///D:/education%20system/my-porject/AGENT_MILESTONES.md)；若有新踩坑点，追加至本文件第四节（`AGENT_WORKLOG.md` 今后严禁记录里程碑）。
