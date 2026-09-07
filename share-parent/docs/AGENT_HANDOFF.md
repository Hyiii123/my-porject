# 在线教育系统交接手册

> 目的：让接手 agent 在不猜测环境、不泄露密钥、不大范围重启服务的前提下，继续开发、联调和发布。
>
> 本文记录截至 `master` 提交 `a5014ab829833c5c0f8e62394dedd2d298d3068c` 的系统状态。本文新增时，同步修正了前端 Nginx 中遗留的 `/device` 反向代理匹配；该路由原本属于已经删除的共享充电宝模块。

## 1. 当前状态与工作约定

- 项目已从共享充电宝方向收敛为在线教育平台。`share-modules/share-device` 已从 Git 删除，网关 `/device/**` 路由、设备常量和管理端旧文案也已删除。
- 当前分支是 `master`，远端是 `origin`：`git@github.com:Hyiii123/my-porject.git`。
- 本地代码目录：`D:\education system\my-porject\share-parent`。
- 线上服务器地址：`47.120.32.166`；线上项目目录：`/opt/tianji/share-parent`。
- 线上访问使用已配置的 Workbench/Codex ECS 会话（支持终端和文件 `upload`/`download`）。认证方式和凭据不写入仓库、聊天记录或本文；接手者应复用已有连接配置，不能臆测 SSH 用户名或复制密钥。
- 这次删减任务遵循的发布顺序是：**先修改服务器源码并验证，再同步到本地，最后提交推送 GitHub**。不要反向把未经验证的 GitHub 代码覆盖服务器；每次同步前先比较差异。

建议每次开始都执行：

```powershell
Set-Location 'D:\education system\my-porject\share-parent'
git fetch origin --prune
git status --short --branch
git log -5 --oneline --decorate
```

服务器连接成功后先执行（只读）：

```bash
cd /opt/tianji/share-parent
git status --short --branch
git log -5 --oneline --decorate
docker compose -p tianji-share ps
```

不要把 `.env`、任何真实第三方 API Key、私钥、数据库导出文件或 Docker 卷传入 Git。

## 2. 目录与模块地图

根目录是一个 Maven 多模块 Spring Cloud 工程；Java 版本固定为 **17**，根 `pom.xml` 已声明 `maven.compiler.source/target=17`。系统用 Spring Boot 3.0.5、Spring Cloud 2022.0.2、Spring Cloud Alibaba 2022.0.0.0-RC2，注册/配置中心是 Nacos。

| 目录 | 内容 | 主要职责 |
| --- | --- | --- |
| `share-gateway` | Spring Cloud Gateway | 全部 API 入口、JWT 鉴权前过滤、跨域、旧接口前缀兼容、服务发现路由 |
| `share-auth` | 认证服务 | `/auth/**` 登录、注册、令牌相关逻辑 |
| `share-api` | 内部 API 契约 | `share-api-system`、`share-api-user`、`share-api-education` 等跨服务 DTO/Feign 契约 |
| `share-common` | 公共库 | core、security、redis、log、datasource、datascope、seata 等，不放领域业务 |
| `share-modules/share-system` | 系统服务 | 用户、角色、菜单、字典、日志等若依基础能力，schema 为 `share` |
| `share-modules/share-user` | 用户服务 | 原有用户相关业务；与系统服务的兼容接口要先确认调用方再改 |
| `share-modules/share-education` | 教育服务 | 课程、教师、目录、学习、考试、问答、笔记、签到、积分、教育工作台，schema 为 `tj_education` |
| `share-modules/share-trade` | 交易服务 | 购物车、订单、演示支付、优惠券、退款、统计，schema 为 `tj_trade` |
| `share-modules/share-customer` | 客服服务 | AI 客服、FAQ、知识库、会话、评价、统计，schema 为 `tj_customer` |
| `share-modules/share-file` | 文件服务 | 上传、文件/媒资元数据，schema 为 `tj_file` |
| `share-modules/share-gen` | 代码生成 | 若依生成器 |
| `share-modules/share-job` | 定时任务 | Quartz 作业 |
| `share-modules/share-rule` | 规则服务 | Drools 规则能力 |
| `share-visual/share-monitor` | 监控服务 | Spring Boot Admin 监控 |
| `share-ui` | 基础管理前端 | Vue 3 + Element Plus，系统管理、日志、监控、代码生成 |
| `frontends/portal` | 用户端前端 | 课程、学习、考试、问答、笔记、订单、优惠券、个人中心、AI 客服 |
| `frontends/business-admin` | 业务管理端 | 课程、媒资、题库、互动、营销、订单、退款、用户、客服工作台 |
| `deploy/nacos` | Nacos Data ID 源文件 | 本地导入和线上配置审查的基线 |
| `sql` | 基础 schema 与增量迁移 | 初始 SQL 和 `sql/migrations/V*.sql` Flyway 迁移 |
| `deploy/smoke-test.ps1` | 自动冒烟测试 | 只读和可写 API 链路验证 |

根 `share-modules/pom.xml` 当前只聚合 `system`、`gen`、`job`、`file`、`user`、`customer`、`education`、`trade`。**不要恢复 `share-device`**，也不要再加 `/device/**` 网关路由。

## 3. 服务、容器与端口

Compose 项目名固定为 `tianji-share`。服务的容器名大多以 `tianji-` 开头。内部服务通过 Docker 网络 `tianji-net`、Nacos 服务名和内部端口通信；宿主机端口主要用于调试，不应用作容器间地址。

| Compose 服务 / 容器 | 内部端口 | 默认宿主机端口 | 责任 |
| --- | ---: | ---: | --- |
| `mysql` / `tianji-mysql` | 3306 | 13306 | MySQL 8，承载逻辑库 `share`、`tj_education`、`tj_trade`、`tj_customer`、`tj_file`、`nacos_config` |
| `redis` / `tianji-redis` | 6379 | 6379 | 登录会话、缓存、限流、客服服务端 Key |
| `nacos` / `tianji-nacos` | 8848 | 8848 | 服务发现与配置；gRPC 映射 9848、9849 |
| `auth` / `tianji-auth` | 9200 | 19200 | 认证 |
| `gateway` / `tianji-gateway` | 18080 | 8080 | 所有 API 统一入口 |
| `system` / `tianji-system` | 9201 | 19201 | 系统管理 |
| `customer` / `tianji-customer` | 9206 | 19206 | AI 客服 |
| `file` / `tianji-file` | 9300 | 19300 | 文件/媒资 |
| `education` / `tianji-education` | 9210 | 19210 | 教育领域 |
| `trade` / `tianji-trade` | 9211 | 19211 | 交易领域 |
| `gen` / `tianji-gen` | 9202 | 19202 | 代码生成 |
| `job` / `tianji-job` | 9203 | 19203 | 作业 |
| `monitor` / `tianji-monitor` | 9100 | 19100 | 监控 |
| `ruoyi-ui` | 80 | 18080 | 基础管理前端 |
| `portal-ui` | 80 | 18081 | 用户端 |
| `business-admin-ui` | 80 | 18082 | 业务管理端 |

本地常用入口：Gateway `http://127.0.0.1:8080`、Nacos `http://127.0.0.1:8848/nacos`、基础管理端 `http://127.0.0.1:18080`、用户端 `http://127.0.0.1:18081`、业务管理端 `http://127.0.0.1:18082`。演示管理员为 `admin / admin123`；不要将这个演示账号当作生产账号。

## 4. 路由与调用边界

网关配置基线是 `deploy/nacos/share-gateway-dev.yml`。前端和脚本都应访问 Gateway，而不是直接访问业务服务。

| 前缀 | 目标服务 | 备注 |
| --- | --- | --- |
| `/auth/**`、`/as/**` | `share-auth` | `/as/**` 是历史前端兼容前缀；`/as/roles` 例外转到 system |
| `/system/**`、`/us/**` | `share-system` | 后者为旧用户前缀兼容 |
| `/code/**`、`/schedule/**` | gen、job | 基础能力 |
| `/file/**`、`/ms/**` | file | 上传/媒资 |
| `/customer/**` | customer | 当前客服接口 |
| `/cs/customer-service/**` | customer | **必须排在通用 `/cs/**` 之前**，保留旧客服兼容 |
| `/cs/**`、`/ss/**`、`/ls/**`、`/es/**`、`/rs/**`、`/ds/**` | education | 课程、学习、考试、互动、教育工作台 |
| `/ts/**`、`/prs/**`、`/ps/**` | trade | 交易和营销 |
| `/rule/**` | rule | 规则 |

路由顺序是功能的一部分：若把 `/cs/customer-service/**` 放到 `/cs/**` 后，客服请求会错误进入教育服务。新增兼容路径时，先在三个前端全仓搜索调用点，再在网关确定具体路径优先级。

所有生产前端的 Nginx 规则在 `frontends/nginx.conf`：

- `/prod-api/`、`/dev-api/` 去前缀后代理到 `gateway:18080`；
- 直接 API 前缀（如 `/auth`、`/cs`、`/ts`）也代理到 Gateway；
- 三个前端默认使用同源 API，避免把构建期的 `localhost` 交给最终用户浏览器解析。

`share-ui` 开发环境经 `VITE_APP_BASE_API=/dev-api` 及 Vite 代理访问 `http://localhost:8080`。`portal` 与 `business-admin` 默认在开发模式调用 `http://localhost:8080`，生产模式使用空同源地址；可用 `VITE_API_BASE_URL` 显式覆盖。不要在生产包中配置 `localhost:8080`。

## 5. 数据库、迁移与 Nacos

### 数据库规则

- 基础建表脚本：`sql/share-system.sql`、`sql/quartz.sql`、`sql/tianji-education.sql`、`sql/tianji-trade.sql`、`sql/tianji-customer.sql`、`sql/tianji-file.sql`。
- 增量变更只能新增 `sql/migrations/V<连续版本>__<说明>.sql`，不能修改已经执行过的迁移文件。
- 新 MySQL 数据卷会执行 `docker-entrypoint-initdb.d` 中的脚本；**已有数据卷不会再次执行**，这是最常见的“SQL 已改但服务没变化”原因。
- 已有环境执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\mysql\apply-migrations.ps1
```

该脚本启动一次性的 Maven/Flyway 容器并写入 `share.flyway_schema_history`，不会重启业务服务。生产迁移前先备份受影响 schema，审阅 SQL 是否幂等、是否会锁大表、是否会影响跨服务逻辑关联。

### Nacos 规则

- 配置文件基线在 `deploy/nacos/*.yml`，例如 `share-education-dev.yml`、`share-customer-dev.yml`、`share-gateway-dev.yml`。
- 本地导入：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\nacos\import-config.ps1
```

- Nacos 使用 MySQL 持久化。配置变动后要确认对应服务是否支持刷新；不能假设所有配置立刻生效。服务启动参数会从 `bootstrap.yml` 的 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE` 读取。
- 线上先查询 Nacos 是否有目标服务实例，再替换服务。尤其做热部署时，不能让旧容器和热容器同时注册为同一个服务。

## 6. 本地开发与构建

### 首次启动或重建底座

```powershell
Set-Location 'D:\education system\my-porject\share-parent'
Copy-Item .env.example .env
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -DskipTests package
docker compose -p tianji-share up -d mysql redis nacos
powershell -ExecutionPolicy Bypass -File .\deploy\nacos\import-config.ps1
docker compose -p tianji-share up -d --build auth gateway system gen job monitor customer file education trade ruoyi-ui portal-ui business-admin-ui
docker compose -p tianji-share ps
```

如果系统默认 Java 是 8，Maven 会因项目要求 Java 17 而失败。不要修改 POM 降级，直接设置 `JAVA_HOME` 到 JDK 17。此问题已实际遇到并验证。

### 常用局部构建

```powershell
# 全量后端编译（当前删减后为 28 个 Maven 模块）
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -DskipTests package

# 三个前端分别构建
Set-Location .\share-ui; npm run build:prod
Set-Location ..\frontends\portal; npm ci; npm run build
Set-Location ..\business-admin; npm ci; npm run build
```

`node_modules`、`target`、`dist` 是生成物，搜索旧业务残留或做 Git 审阅时排除它们。它们可能包含 `station`、`charging` 等第三方通用词，不是项目业务代码。

## 7. ECS 发布与低中断联调

### 7.1 发布前检查

在服务器项目目录中，先做只读检查：

```bash
cd /opt/tianji/share-parent
docker compose -p tianji-share ps
docker compose -p tianji-share logs --tail=100 gateway
curl -fsS http://127.0.0.1:8080/actuator/health
```

修改范围要小：后端改动只重建目标服务和其确实需要同步的依赖；前端改动只重建对应 UI 容器；纯 Nacos 内容不要顺带重建整个系统。使用绝对项目目录避免在错误的 Compose 项目下操作：

```bash
docker compose --project-directory /opt/tianji/share-parent -p tianji-share up -d --build education
# 例如仅重建教育服务；将 education 换成实际目标服务。
```

不要执行 `docker compose down -v`，不要删除数据库/Redis/Nacos volume，不要批量重启所有服务来掩盖单服务问题。

### 7.2 已落地的客服服务热部署

现有“无容器重启”能力只为 `share-customer` 固化，文件为 `docker-compose.hot.yml` 和 `deploy/hot-run.sh`。服务器没有预装 JDK、Maven、Node；热容器使用 Maven 3.9 + Temurin 17 镜像，在挂载的服务器源码目录编译。

工作方式：

1. 停止正式 `customer` 服务。
2. 启动 `customer-hot`。它使用同一个源码挂载和 Maven cache volume `tianji_maven_cache`。
3. `hot-run.sh` 每 2 秒检测 `share-customer/src`、`share-common`、`share-api` 中的 Java/XML/YAML/properties 变更。
4. 变化后执行 `mvn -pl share-modules/share-customer -am -Phot-reload ... compile`，再 touch `target/classes/reload.trigger`。
5. Spring Boot DevTools 监听触发文件并重载 Spring 上下文，Docker 容器不重启。

正确切换顺序如下：

```bash
# 正式 customer 与 hot customer 不能同时注册到 Nacos。
docker compose --project-directory /opt/tianji/share-parent -p tianji-share stop customer
docker compose --project-directory /opt/tianji/share-parent \
  -f /opt/tianji/share-parent/docker-compose.hot.yml -p tianji-hot up -d customer-hot

# 日志中确认编译成功、Nacos 注册完成后，再通过 Gateway 做 API 验证。
docker logs -f tianji-customer-hot

# 验证完成，恢复正式服务。
docker compose --project-directory /opt/tianji/share-parent \
  -f /opt/tianji/share-parent/docker-compose.hot.yml -p tianji-hot stop customer-hot
docker compose --project-directory /opt/tianji/share-parent -p tianji-share start customer
```

热部署期间，若 Maven 编译失败，脚本保留上一次可运行 classes；先修编译错误，不要通过重启容器掩盖它。修改共享 API/公共库时，`-am` 会重编所需依赖。涉及 Dockerfile、环境变量、启动参数、依赖版本、Compose 或数据库迁移时不适合热重载，应按单服务重建或迁移流程执行。

### 7.3 前后端联调而不频繁重启

- **本地前端**：运行 Vite 开发服务器，源文件保存即 HMR。`portal` 用 `npm run dev`，`business-admin` 用 `npm run dev:prod`；不要每改一个 Vue 文件就构建 Docker 镜像。两个 Vite 配置当前都默认使用 `18081`，需要并行运行时为后启动的业务管理端追加 `-- --port 18083`，例如 `npm run dev:prod -- --port 18083`。
- **本地后端**：优先只启动/重建被改服务，前端始终指向 Gateway `8080`。变更教育接口时重启/替换 education，不要重启 auth、trade、customer。
- **服务器客服改动**：采用上述 `customer-hot` 机制；网关继续经 Nacos 发现唯一 `share-customer` 实例。
- **服务器其他后端改动**：当前没有通用热容器模板。验证通过后只执行目标服务 `up -d --build <service>`；避免全站重启。
- **生产前端改动**：静态 Nginx 容器不会自动读取服务器源码。只重建对应 UI 镜像/容器，并用浏览器强制刷新验证；Nginx 已对静态资源设置长缓存，因此若文件名不带 hash 或 CDN 在前，需要额外核查缓存策略。`frontends/nginx.conf` 是 `ruoyi-ui`、`portal-ui`、`business-admin-ui` 三个镜像共同 COPY 的模板。**严禁在服务器上一条命令同时并发构建多个前端服务**（例如 `up -d --build ruoyi-ui portal-ui business-admin-ui`），因为并发执行 `npm ci` 与多进程 Vite 打包会瞬间解压数万个碎片小文件，迅速耗尽云服务器云盘的突发 IO 积分（Burst I/O Credits），导致磁盘硬件级限流、CPU 高 iowait、数据库与网关超时假死！若需更新多个前端，必须**严格串行排队构建**（先构建一个，等待启动并平息后再构建下一个），或者优先在本地构建好静态产物后仅同步产物。

## 8. 验收与诊断入口

最小健康检查：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\smoke-test.ps1
```

可写测试会写入或复用演示数据（报名、学习记录、兑换、订单、客服会话/评价），只在开发或测试环境运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\smoke-test.ps1 -IncludeWriteFlow
```

脚本覆盖网关健康、管理员和学员登录、课程/学习/交易/媒资/客服查询，以及可写客服、订单支付、学习记录链路。它还兼容 Windows PowerShell 5.1 对 JSON 中 `username`/`userName` 大小写重复字段的解析缺陷；不要随意删掉 `Convert-ApiJson`。

已验证的结果：

- 历史全量 Maven 构建为 29 个模块通过；删除设备模块后重新验证为 28 个模块通过。
- `share-ui`、`portal`、`business-admin` 的生产构建均通过。
- 只读 HTTP 冒烟 35 项通过；完整可写 HTTP 冒烟 62 项通过。
- ECS 扩展只读联调 35 项和可写联调 32 项通过，期间未重启业务容器。
- 普通角色访问客服统计接口返回业务 `403`；管理员可访问知识库、FAQ、会话和统计。
- 未运行真实 Pixel Key 的在线调用，以避免泄露密钥和产生外部费用；无 Key 时已验证 FAQ/知识库降级回复。

对单个服务故障按以下顺序排查：Gateway health → Nacos 是否有唯一健康实例 → 该服务容器日志 → 对应 schema/migration → Gateway 路由顺序与前端 base URL。不要直接从前端错误推断为数据库或服务代码问题。

## 9. AI 客服安全边界

- Pixel 目标地址为 `https://api.ai-pixel.online/v1/responses`，不是 OpenAI 官方 API。
- 用户端临时 Key 仅可保存在浏览器 `localStorage`，发送请求时由客服服务临时代理；不得写入数据库、Redis、日志或 SQL。
- 管理端 Key 目前写 Redis `customer:ai:secret`；生产应接入密钥管理服务。
- 未配置 Key、第三方失败或响应不兼容时，客服应继续使用 MySQL FAQ/知识库降级，不应让用户端崩溃。
- 客服需求不包含人工转接。旧 `/cs/customer-service/*` 路径只做兼容，不能偷偷恢复人工坐席/人工回复语义。

## 10. 本轮删减的精确记录

提交 `a5014ab8` 完成共享充电宝模块下线：

- 删除 `share-modules/share-device` 下 58 个源码、Mapper、MQTT/EMQX、资源文件。
- 从 `share-modules/pom.xml` 移除模块。
- 删除 `share-common-core` 的 `DeviceConstants`。
- 从 `UserVo` 删除仅服务旧押金流程的 `depositStatus`。
- 从 Nacos gateway 配置删除 `lb://share-device` 与 `/device/**`。
- 管理端 `share-ui` 的系统名称、登录/注册页与首页旧文案改为在线教育。
- 本文提交同步删除 `frontends/nginx.conf` 的 `/device` 代理匹配，防止已经下线的前缀继续进入网关。

服务器已检查：Nacos 没有 `share-device` 实例、运行容器没有设备服务、数据库未发现 `power_bank`、`cabinet`、`station` 等旧业务表，服务器全量 Maven 构建通过。代码审查时，编译残留 `share-modules/share-device/target` 如果出现，应视为 `.gitignore` 忽略的本地构建产物，不应提交；清理它不会影响 Git 或线上运行。

以后做旧业务清理，请同时搜索：Maven modules、Gateway/Nacos routes、Nginx 代理正则、前端文案/菜单、API DTO、SQL、Docker Compose、运行容器和 Nacos 实例。只删 Java 目录并不算完成。

## 11. Git 与服务器同步工作流

这是当前用户明确要求的工作流，适用于线上先试验、确认再归档的变更：

1. 在服务器 `/opt/tianji/share-parent` 做最小源码改动。通过 Workbench CLI 只上传所需文件；不要覆盖 `.env`、`target`、`node_modules`、`dist`。
2. 在服务器构建目标服务或全量 Maven，必要时以单服务方式替换/热切换，并经 Gateway 冒烟测试。
3. 下载服务器实际修改回本地对应路径，逐个查看 `git diff`；不要用目录覆盖替换整个本地工作树。
4. 在本地设置 JDK 17 后重跑与改动匹配的后端/前端构建和残留扫描。
5. `git fetch origin --prune`，确认 `master` 无远端新增提交；若有新增，先审阅并正常合并/rebase，禁止强推。
6. `git diff --check`、`git diff --name-status`、`git status --short` 审查后再 `git add -A`。
7. 使用语义明确的提交信息，例如 `refactor: remove shared power bank module`，然后 `git push origin master`。
8. 用 `git rev-parse HEAD` 与 `git ls-remote origin refs/heads/master` 比较哈希，确认 GitHub 已收到同一个提交。

本轮最终远端确认提交为 `a5014ab829833c5c0f8e62394dedd2d298d3068c`。本文档的后续提交会在该提交之上，交接时以 `git log -1` 为准。

## 12. 已遇到的卡点与避免方式

| 现象 | 根因 | 固定处理方式 |
| --- | --- | --- |
| Maven 在 Windows 上构建失败 | 默认 Java 8，项目需要 Java 17 | 临时设置 `JAVA_HOME` 和 `Path` 到 `C:\Program Files\Java\jdk-17`，不要改 POM 迁就旧 JDK |
| 改了 SQL 但线上/本地库没有变化 | 已存在 MySQL volume 不会重跑初始化 SQL | 新增 Flyway `V*.sql`，运行 `apply-migrations.ps1`，查 `share.flyway_schema_history` |
| 页面在生产环境访问 API 失败或指向访客电脑 | 构建包包含 `localhost:8080` | 生产默认同源，Nginx 代理到 `gateway:18080`；只在本地开发使用 localhost |
| `/cs/customer-service` 接口进入教育服务 | Gateway 通用 `/cs/**` 路由排在特例之前 | 保持客服特例在通用教育路由前，并添加网关测试 |
| 热部署后请求被两个不同版本处理 | 正式 customer 与 hot customer 同时注册 Nacos | 严格先 stop customer，再启动 customer-hot；恢复时反向切换 |
| 每次热改都很慢 | Maven 重复拉依赖 | 使用 Compose 命名卷 `tianji_maven_cache`，不要删除它作为日常清理 |
| Windows PowerShell 冒烟脚本 JSON 解析报大小写冲突 | 遗留接口同时出现 `username` 和 `userName` 类字段 | 保留 `System.Web.Script.Serialization.JavaScriptSerializer` 兼容解析 |
| 同时启动 portal 与 business-admin 的本地 Vite | 两个配置默认监听 `18081` | 为业务管理端执行 `npm run dev:prod -- --port 18083`，或只启动当前调试的一个 |
| 修改一个后端服务导致全站长时间中断 | 习惯性 `restart`/`down` 全部容器 | 目标服务构建与定向重建；客服优先 hot 容器；禁止 `down -v` |
| 以为设备代码已清完仍有可达入口 | 只删了 Maven 模块，漏了 Nginx 代理或配置 | 清理后扫描模块、Nacos、Nginx、Docker、SQL、DTO 和运行时实例；本文已移除遗留 `/device` 正则 |
| 搜索出现 charging/station 等误报 | 命中 `node_modules` 或编译产物的第三方内容 | 残留扫描排除 `node_modules`、`target`、`dist`、`.git`，再人工判断领域语义 |
| Git 看起来有旧设备目录 | `target` 是被忽略的构建产物 | 用 `git status --ignored`、`git check-ignore -v` 区分源码与生成物；不要误把它添加进提交 |
| 在服务器并发构建多个前端/服务导致超时或假死 | Docker 并发执行 `npm ci` 和多路 Vite 编译打包，瞬间产生数万小文件写 I/O，耗尽云盘突发积分并触发硬件级限流（I/O Throttling） | **绝对禁止并发构建**；多服务必须**串行排队构建**（`docker compose ... up -d --build svc1` 等启动平息后再 `svc2`）；或优先**在本地打出静态产物包，服务器只做纯文件同步/镜像封装** |

## 13. 接手后的第一小时清单

1. 确认本地 `master`、远端哈希、工作树和 Java 17。
2. 通过既有 Workbench ECS 会话确认服务器目录、Docker 服务、Gateway health、Nacos 实例。
3. 阅读与任务有关的模块、Nacos Data ID、Gateway 路由和前端 API 文件，不跨服务猜测接口。
4. 先运行只读冒烟测试，记录基线；涉及写入时使用专用测试环境或明确批准后运行 `-IncludeWriteFlow`。
5. 按“服务器验证 → 本地同步验证 → GitHub”顺序执行，提交前跑 `git diff --check` 和相关构建。
6. 交付时报告：改动文件、未改动的服务、构建/冒烟结果、服务器容器状态、提交哈希、已知未验证项。
