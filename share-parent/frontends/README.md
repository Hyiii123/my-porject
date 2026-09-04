# 天机学堂前端迁移应用

这里保留并接入原有的两个 Vue 前端，作为 `share-parent` 的增量前端模块：

- `portal`：用户端，课程、学习、考试、问答、笔记、订单、优惠券、个人中心和 AI 客服。
- `business-admin`：业务管理端，课程、媒资、题库、互动、营销、订单、退款、用户和客服管理。

两个应用的 API 都通过 `VITE_API_BASE_URL` 指向 Spring Cloud Gateway，生产构建默认使用 `http://localhost:8080`。页面不再启用原有 Mock 适配器；业务数据由 `share-education`、`share-trade`、`share-customer`、`share-file` 和 `share-system` 服务提供。

本地开发：

```powershell
cd frontends/portal
npm ci
npm run dev

cd ../business-admin
npm ci
npm run dev:prod
```

容器构建由根目录 `docker-compose.yml` 统一完成。
