# 智问学伴前端迁移应用

这里保留并接入原有的两个 Vue 前端，作为 `share-parent` 的增量前端模块：

- `portal`：用户端，课程、学习、考试、问答、笔记、订单、优惠券、个人中心和 AI 客服。
- `business-admin`：业务管理端，课程、媒资、题库、互动、营销、订单、退款、用户和客服管理。

两个应用的 API 都通过 `VITE_API_BASE_URL` 指向 Spring Cloud Gateway，生产构建默认使用当前站点的同源地址，并由 Nginx 转发到网关；本地开发未设置该变量时使用 `http://localhost:8080`。页面不再启用原有 Mock 适配器；业务数据由 `share-education`、`share-trade`、`share-customer`、`share-file` 和 `share-system` 服务提供。

## 架构升级：pnpm workspace Monorepo 统一架构

前端工程已全面升级为基于 **pnpm workspace** 的 Monorepo 体系，并在 `frontends/packages/shared` 下提供统一的 `@zhiwen/shared` 核心共享包：
- **`@zhiwen/shared`**：统一封装通用常量（Token 标识、默认头像/封面、订单状态、面试轮次枚举）、统一鉴权与双 Token 静默刷新工具（`tryRefreshToken`）、通用日期与校验工具、全局 SCSS 变量与 Mixins。
- **`portal`**：面向学员的业务前台。
- **`business-admin`**：面向教务/运营的业务后台。
- **`share-ui`**：平台超级管理后台（若联调时作为同级工作区接入）。

### 常用命令

#### 1. 工作区根目录（`frontends/`）统一管理
```powershell
cd frontends

# 一键安装所有前端子项目依赖
pnpm install

# 一键并发/按序打包所有应用
pnpm -r build
```

#### 2. 单独子应用构建与开发（完全保持向后兼容）
```powershell
# 用户端 portal
cd frontends/portal
pnpm run dev
pnpm run build

# 业务管理端 business-admin
cd frontends/business-admin
pnpm run dev
pnpm run build
```

容器构建与静态产物挂载由根目录 `docker-compose.yml` 统一完成。
