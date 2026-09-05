# 天机学堂迁移与运行说明

## 1. 分析结论

原项目实际是两个 Vue 3 前端项目，不是 Python 全栈项目：

- `frontends/portal`：用户端，保留课程、学习、考试、问答、笔记、订单、优惠券和个人中心页面，并接入整页 AI 客服中心。
- `frontends/business-admin`：业务管理端，保留课程、媒资、题库、互动、营销、订单、退款和用户管理页面，并接入客服管理工作台。
- `share-ui`：原若依基础管理端，保留系统用户、角色、菜单、字典、日志、监控、代码生成等能力，并在顶部加入客服中心入口。
- `share-parent`：原有若依 Spring Cloud 底座，作为后端基础工程增量扩展，原源码和原始静态数据均保留。

当前需求不包含转人工客服，因此没有新增人工坐席、人工回复或转人工接口。客服会话只有 AI 服务和已结束两种业务状态。

## 2. 微服务边界

| 服务 | 端口 | 职责 | 数据边界 |
| --- | ---: | --- | --- |
| `share-gateway` | 18080（宿主机 8080） | 路由、跨域、鉴权、旧路径兼容 | 无业务表 |
| `share-auth` | 9200 | 登录、注册、JWT/Redis 会话 | `share` |
| `share-system` | 9201 | 若依用户、角色、菜单、日志 | `share` |
| `share-education` | 9210 | 课程、分类、教师、目录、学习、考试、问答、笔记、积分 | `tj_education` |
| `share-trade` | 9211 | 购物车、订单、优惠券、退款、交易统计 | `tj_trade` |
| `share-customer` | 9206 | AI 客服、知识库、FAQ、会话、评价、统计 | `tj_customer` |
| `share-file` | 9300 | 文件和媒资元数据、上传签名 | `tj_file` |

本地 Compose 使用一个 MySQL 实例承载多个逻辑数据库。服务只访问自己的 schema，不直接操作其他服务的业务表；`user_id`、`course_id`、`order_id` 等跨服务字段采用逻辑关联。

## 3. 原功能到新服务的映射

| 原功能 | 新服务 | 兼容前缀 | 主要持久化表 |
| --- | --- | --- | --- |
| 首页、分类、课程、教师、课程目录 | `share-education` | `/cs/**` | `edu_banner`、`edu_category`、`edu_course`、`edu_teacher`、`edu_course_catalog` |
| 管理工作台今日指标、访问趋势和热门课程 | `share-education` | `/ds/data/**` | `edu_dashboard_daily`、`edu_course` |
| 学习记录、学习计划、签到、积分 | `share-education` | `/ls/**` | `edu_learning_record`、`edu_learning_plan`、`edu_sign_record`、`edu_points_ledger` |
| 问答、回复、点赞、笔记、收藏 | `share-education` | `/cs/**`、`/rs/**` | `edu_question`、`edu_reply`、`edu_note`、关联表 |
| 考试、题库、考试记录 | `share-education` | `/es/**` | `edu_exam`、`edu_exam_question`、`edu_exam_record`、`edu_question_bank` |
| 购物车、订单、支付演示、退款 | `share-trade` | `/ts/**` | `trade_cart`、`trade_order`、`trade_order_detail`、`trade_refund_apply` |
| 优惠券、营销 | `share-trade` | `/prs/**`、`/ps/**` | `mkt_coupon`、`mkt_user_coupon` |
| 文件、图片、媒资 | `share-file` | `/file/**`、`/ms/**` | `sys_file`、`media_file` |
| AI 客服会话和评价 | `share-customer` | `/customer/**`、旧 `/cs/customer-service/**` | `cs_session`、`cs_message`、`cs_evaluation` |
| 知识库和常见问题维护 | `share-customer` | `/customer/admin/**` | `cs_knowledge`、`cs_faq` |
| 客服统计和 AI 配置 | `share-customer` | `/customer/admin/**` | `cs_ai_config`、`cs_ai_call_log` |
| 系统管理 | `share-system` | `/system/**`、`/us/**` | 若依原有 `sys_*` 表 |

本轮兼容性修复：

- 小节学习记录接口 `GET /ls/learning-records/lessons/{lessonId}` 按当前用户和 `catalog_id` 查询，避免把小节编号误当成课程编号。
- 兑换码兑换按真实兑换码查询，并校验兑换码、优惠券状态和有效期；领取动作使用条件更新控制并发重复兑换，同一用户重复请求保持幂等。
- 题库列表补充旧路径 `GET /es/questions/list`，与 `/es/questions/page` 返回相同分页结构，避免被 `/questions/{id}` 当作数字编号解析。

## 4. 客服接口

用户端新接口：

- `POST /customer/session`：创建 AI 会话。
- `GET /customer/session/my`：查询当前用户会话。
- `GET /customer/session/{id}`：查询会话和消息详情。
- `GET /customer/session/{id}/messages`：查询消息。
- `POST /customer/session/{id}/messages`：服务端调用 Pixel AI；未启用或调用失败时使用本地知识库降级。
- `POST /customer/session/{id}/messages/record`：历史兼容接口，用于旧页面已经完成第三方调用后的记录回写；新用户端不再直连第三方。
- `POST /customer/session/{id}/evaluation`：提交 1~5 分服务评价，提交后关闭会话。
- `GET /customer/faq/public`：公开 FAQ。

管理端新接口：

- `/customer/admin/knowledge/**`：知识库分页、详情、新增、修改、删除。
- `/customer/admin/faq/**`：FAQ 分页、详情、新增、修改、删除。
- `/customer/admin/sessions/**`：会话分页、详情、消息和关闭。
- `GET /customer/admin/statistics/overview`：客服统计、趋势和高频问题。
- `/customer/admin/ai/config`：第三方 Pixel AI 配置查询和更新。
- `POST /customer/admin/ai/test`：第三方 Pixel AI 连通性测试。

旧 `/cs/customer-service/*` 接口仍由兼容控制器承接，但不包含转人工和人工回复动作。

## 5. Pixel AI 配置

项目只允许第三方 Pixel 地址：

```text
https://api.ai-pixel.online/v1/responses
```

没有配置 GPT 官方 API。模型、超时、重试次数和系统提示词可以通过管理端或 Nacos/环境变量配置。API Key 不写入 SQL 初始化脚本，也不通过配置接口回显：

- 用户端输入的 Key 只保存在当前浏览器 `localStorage`，发送消息时通过本站 `/customer/session/{id}/messages` 临时转交客服服务；客服服务端只在当前请求中代理第三方调用，不把 Key 写入 MySQL、Redis 或操作日志。
- 用户端输入 Key 时，服务端代理优先使用该 Key；未输入时使用管理端 Redis/环境变量中的服务端 Key。
- 管理端配置的 Key 只保存到 Redis `customer:ai:secret`，供客服服务端调用。
- 未配置 Key、第三方调用失败或返回格式无法解析时，客服服务自动查询 MySQL FAQ/知识库并给出降级回复。

## 6. Redis 使用

| Key | 类型 | TTL | 用途 |
| --- | --- | ---: | --- |
| `login_tokens:{uuid}` | String | JWT 会话周期 | 若依登录会话 |
| `cs:faq:list:{limit}` | String | 5 分钟 | 热门 FAQ 列表缓存 |
| `cs:rate:ask:{userId}` | String counter | 60 秒 | 单用户客服请求限流，最多 10 次 |
| `customer:ai:secret` | String | 持久 | 本地演示环境的服务端第三方 Key；生产环境应改用密钥管理服务 |

FAQ 新增、修改和删除会清理 `cs:faq:list:*`。Redis 暂时不可用时不阻断 MySQL 查询和本地知识库降级，但请求限流也会按“放行并记录日志”处理，生产环境可改为 fail-closed。

## 7. 数据迁移

首次创建 MySQL 数据卷时，Compose 会按顺序执行基础建表脚本和兼容种子脚本：

1. `sql/share-system.sql`、`sql/quartz.sql`。
2. `sql/tianji-education.sql`、`sql/tianji-trade.sql`、`sql/tianji-customer.sql`、`sql/tianji-file.sql`。
3. 现有 `sql/migrations/V*.sql` 会在初始数据卷中提供兼容数据；版本历史由 Flyway
   迁移任务补齐并持续管理。

已有 MySQL 数据卷不会重复执行 entrypoint 初始化脚本。现在应在项目根目录执行
Flyway 一次性迁移任务：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\mysql\apply-migrations.ps1
```

该脚本通过 `docker-compose.migrate.yml` 使用 Maven/Flyway 容器执行，不会启动或重启
业务服务；Flyway 历史记录写入 `share.flyway_schema_history`。迁移密码只从 Compose
环境变量传入，不写入命令行、SQL 或日志。迁移脚本均按幂等方式编写，使用
`INSERT ... ON DUPLICATE KEY UPDATE`、`INSERT IGNORE` 或条件更新。执行失败时脚本会
立即停止；正式环境应先备份对应 schema，再在事务或备份恢复策略下执行。

学习记录和兑换码的写链路可以使用以下脚本验证。只读模式不会写入业务数据，`-IncludeWriteFlow` 会创建或复用演示报名、学习记录、兑换和订单支付数据：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\smoke-test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\smoke-test.ps1 -IncludeWriteFlow
```

校验示例：

```sql
SELECT COUNT(*) FROM tj_customer.cs_knowledge WHERE del_flag = 0;
SELECT COUNT(*) FROM tj_customer.cs_faq WHERE del_flag = 0;
SELECT COUNT(*) FROM tj_education.edu_course WHERE del_flag = 0;
SELECT COUNT(*) FROM tj_education.edu_dashboard_daily;
SELECT COUNT(*) FROM tj_trade.trade_order WHERE del_flag = 0;
SELECT COUNT(*) FROM share.sys_menu WHERE menu_id BETWEEN 2200 AND 2215;
```

## 8. 本地启动

```powershell
Copy-Item .env.example .env
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -DskipTests package
docker compose -p tianji-share up -d
powershell -ExecutionPolicy Bypass -File .\deploy\nacos\import-config.ps1
docker compose -p tianji-share restart auth gateway system education trade customer
```

访问地址：

- 若依基础管理端：`http://localhost:18080`
- 用户端：`http://localhost:18081`
- 业务管理端：`http://localhost:18082`
- Gateway：`http://localhost:8080`
- Nacos：`http://localhost:8848/nacos`

演示账号密码：`admin / admin123`。当前 Docker Compose 使用公共国内镜像源，不需要登录阿里云账号；可在 `.env` 中通过 `TJ_DOCKER_REGISTRY` 替换镜像源。

## 9. ECS CLI 热部署联调

服务器没有安装 JDK、Maven 和 Node，使用本地 Workbench CLI 连接 ECS，服务器上的
`docker-compose.hot.yml` 通过 Maven/JDK 17 容器编译客服服务。热服务挂载服务器源码目录，
启用 Spring Boot DevTools；源码变更后只重新编译并触发上下文重载，不重启 Docker 容器。

生产服务与热服务必须按顺序切换，只允许一个 `share-customer` 实例注册到 Nacos：

```bash
docker compose --project-directory /opt/tianji/share-parent -p tianji-share stop customer
docker compose --project-directory /opt/tianji/share-parent \
  -f /opt/tianji/share-parent/docker-compose.hot.yml -p tianji-hot up -d customer-hot
```

热服务验证完成后恢复正式容器：

```bash
docker compose --project-directory /opt/tianji/share-parent \
  -f /opt/tianji/share-parent/docker-compose.hot.yml -p tianji-hot stop customer-hot
docker compose --project-directory /opt/tianji/share-parent -p tianji-share start customer
```

代码文件通过 Workbench CLI 的 `upload`/`download` 传输；不要上传服务器 `.env`，也不要把
第三方 Pixel API Key 写入仓库。Maven 依赖缓存保存在 `tianji_maven_cache` 卷中，避免每次
热部署重复下载。

## 10. 验收记录

已执行：

- Maven 客服模块测试：5 项通过。
- Maven 全量 `mvn -DskipTests package`：29 个模块构建通过。
- `share-ui` 执行 `npm run build:prod`、`frontends/portal` 和 `frontends/business-admin` 执行 `npm run build`：三个前端构建通过。
- 只读 HTTP 冒烟测试：35 项通过；包含网关健康检查、双角色登录、课程/学习/交易/媒资/客服管理查询。
- 完整 HTTP 写链路冒烟测试：62 项通过；在基础业务写链路之外，还覆盖 AI 客服会话创建、无 Key 时的 FAQ/知识库降级回复、客服消息历史、服务评价、评价后自动关闭会话，以及管理端会话和消息查询。
- ECS 通过 Workbench CLI 执行扩展只读联调：35 项全部通过，覆盖用户端考试、积分、签到、问答、笔记和管理端教育/交易/客服接口。
- ECS 通过 Workbench CLI 执行扩展可写联调：32 项全部通过，覆盖问答回复与点赞、笔记全生命周期、考试提交幂等、签到幂等、退款审批和旧客服兼容接口；未重启业务容器。
- 实际 HTTP 链路：学生登录 → 创建会话 → 知识库回复 → 评价 → 刷新查询，返回均为业务成功，评价后状态为 `3`（已关闭）。
- 实际 HTTP 链路：用户端登录 → 创建客服会话 → 无 Key 知识库降级 → 带临时 Key 的服务端代理入口，均返回业务成功；真实第三方 Key 未写入仓库。
- 管理端知识库、FAQ、会话和统计接口通过管理员权限访问。
- 普通角色调用统计接口返回业务码 `403`。

未执行：真实 Pixel API Key 的在线连通性测试；项目没有在仓库中配置真实 Key，避免泄露和产生外部费用。
