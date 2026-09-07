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
