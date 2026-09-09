# Agent Mandatory Operating Rules (智问学伴项目智能体守则)

## 0. MANDATORY PRE-FLIGHT CHECK (每次工作前必读)
Before performing any coding, file modification, server command, build, or debugging task, the agent **MUST ALWAYS read**:
1. [`AGENT_WORKLOG.md`](file:///D:/education%20system/my-porject/AGENT_WORKLOG.md) (Worklog, key milestones, difficulties, known pitfalls, checklists)
2. [`share-parent/docs/AGENT_HANDOFF.md`](file:///D:/education%20system/my-porject/share-parent/docs/AGENT_HANDOFF.md) (Project architecture, module map, ports, route boundaries)

Failure to read and follow these documents before acting is a violation of user rules.

## 1. Golden Release Workflow (发布铁律)
Always follow the unidirectional flow:
**Modify & test on server first ➔ verify with zero issues (smoke test 35 pass) ➔ sync back to local ➔ review git diff ➔ commit & push to GitHub**.
Never push unverified code or overwrite the server with unverified local/GitHub code.

## 2. Cloud Disk Protection Rule (云盘保护与禁止并发构建)
**STRICTLY FORBIDDEN**: Running concurrent builds on the server (e.g. `docker compose up -d --build ruoyi-ui portal-ui business-admin-ui`).
Alibaba Cloud ECS ESSD Entry disks have limited base IOPS (1800-3000). Concurrent builds deplete burst credits, triggering hardware I/O throttling, high iowait, and service crashes.
**RULE**: All builds on the server MUST be strictly sequential (one by one), OR built locally as static `dist/` artifacts and synced directly.

## 3. Minimal Disruption & Database Safety
**STRICTLY FORBIDDEN**: `docker compose down -v`. Never delete MySQL, Redis, or Nacos volumes.
Only rebuild/restart the target service container modified.

## 4. Worklog Maintenance
Whenever work is done (new milestones, bug fixes, resolved pitfalls), the agent MUST update [`AGENT_WORKLOG.md`](file:///D:/education%20system/my-porject/AGENT_WORKLOG.md) to record the changes.

## 5. Prohibition of Local Service Execution (禁止在本地启动服务铁律)
**STRICTLY FORBIDDEN**: Starting backend microservices (e.g. `java -jar`, `mvn spring-boot:run`, local IDE service runs) or middleware (MySQL, Redis, Nacos, local Docker Compose) on the local developer machine.
All application runtime services, databases, Redis, and Nacos run exclusively on the remote cloud server (Alibaba Cloud ECS `47.120.67.187`). Local environment is strictly reserved for:
- Code editing, diff review, and version control (`git`).
- Static offline compilation / packaging (`mvn compile / package`, `npm run build`).
- Remote CLI operations and verification tests against the server (Workbench CLI, smoke test targeting `http://47.120.67.187:8080`).
No local port listeners or local service instances are allowed.

