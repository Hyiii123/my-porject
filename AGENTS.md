# Agent Mandatory Operating Rules (智问学伴项目智能体守则)

## 0. MANDATORY PRE-FLIGHT CHECK (每次工作前必读)
Before performing any coding, file modification, server command, build, or debugging task, the agent **MUST ALWAYS read**:
1. [`AGENT_WORKLOG.md`](file:///D:/education%20system/my-porject/AGENT_WORKLOG.md) (Worklog, environment indices, known pitfalls, checklists)
2. [`AGENT_MILESTONES.md`](file:///D:/education%20system/my-porject/AGENT_MILESTONES.md) (Architecture evolution, completed milestones, key deliverables)
3. [`share-parent/docs/AGENT_HANDOFF.md`](file:///D:/education%20system/my-porject/share-parent/docs/AGENT_HANDOFF.md) (Project architecture, module map, ports, route boundaries)

Failure to read and follow these documents before acting is a violation of user rules.

## 1. Golden Release Workflow (发布铁律)
Always follow the unidirectional flow:
**Modify & test on server first ➔ targeted verification of modified parts with zero issues ➔ sync back to local ➔ review git diff ➔ commit & push to GitHub**.
Never push unverified code or overwrite the server with unverified local/GitHub code.

## 2. Cloud Disk Protection Rule (云盘保护与禁止并发构建)
**STRICTLY FORBIDDEN**: Running concurrent builds on the server (e.g. `docker compose up -d --build ruoyi-ui portal-ui business-admin-ui`).
Alibaba Cloud ECS ESSD Entry disks have limited base IOPS (1800-3000). Concurrent builds deplete burst credits, triggering hardware I/O throttling, high iowait, and service crashes.
**RULE**: All builds on the server MUST be strictly sequential (one by one), OR built locally as static `dist/` artifacts and synced directly.

## 3. Minimal Disruption & Database Safety
**STRICTLY FORBIDDEN**: `docker compose down -v`. Never delete MySQL, Redis, or Nacos volumes.
Only rebuild/restart the target service container modified.

## 4. Worklog & Milestone Maintenance (工作日志与里程碑维护准则)
- **日常避坑与运维记录**：Whenever major pitfalls are resolved, environment indices change, or SOP workflows are updated, update [`AGENT_WORKLOG.md`](file:///D:/education%20system/my-porject/AGENT_WORKLOG.md).
- **重大里程碑专属记录**：Whenever substantial engineering deliverables, architectural evolution, dataset migrations, or technical breakthroughs are completed, the agent MUST record them **EXCLUSIVELY in [`AGENT_MILESTONES.md`](file:///D:/education%20system/my-porject/AGENT_MILESTONES.md)**. `AGENT_WORKLOG.md` is strictly forbidden from recording milestones going forward.
- **STRICTLY FORBIDDEN**: Recording routine operations (e.g. simply starting/stopping services, checking container status, running basic tests) into "重大里程碑 (Milestones)". Milestones are strictly reserved for real engineering deliverables, architectural evolution, dataset migrations, and technical breakthroughs.

## 5. Prohibition of Local Service Execution (禁止在本地启动服务铁律)
**STRICTLY FORBIDDEN**: Starting backend microservices (e.g. `java -jar`, `mvn spring-boot:run`, local IDE service runs) or middleware (MySQL, Redis, Nacos, local Docker Compose) on the local developer machine.
All application runtime services, databases, Redis, and Nacos run exclusively on the remote cloud server (Alibaba Cloud ECS `47.120.67.187`). Local environment is strictly reserved for:
- Code editing, diff review, and version control (`git`).
- Static offline compilation / packaging (`mvn compile / package`, `npm run build`).
- Remote CLI operations and verification tests against the server (Workbench CLI, smoke test targeting `http://47.120.67.187:8080`).
No local port listeners or local service instances are allowed.

## 6. Prohibition of C Drive Usage (本地严禁占用C盘铁律)
**STRICTLY FORBIDDEN**: Using the C: drive for temporary scripts, downloaded caches, scratch files, or large outputs.
The C: drive has restricted space. All local operations, temporary scripts, crawl data, and intermediate files MUST strictly use the D: drive project space (e.g. `d:\education system\my-porject\.scratch\`, which is gitignored). Any scratch files on C: drive must be wiped immediately.

## 7. Daily Public IP Verification (每日公网 IP 变更检测铁律)
**BACKGROUND**: The ECS instance uses a dynamic public IP that may change after a server restart.
**RULE**: Once per calendar day (before the first server-related task), the agent MUST run:
```
workbench exec --instance-id i-f8z1loc07p8p5ve8c7jf --command "curl -s http://100.100.100.200/latest/meta-data/eipv4"
```
Compare the returned IP against the recorded IP in `AGENT_WORKLOG.md` (Section 二, "ECS 公网 IP" row).
- **If unchanged**: No action needed. Note the check was done for the day.
- **If changed**: Immediately update ALL hardcoded IP references across the project:
  - `AGENTS.md` (Rules 5, this file)
  - `AGENT_WORKLOG.md` (Section 二 environment table + any references)
  - `share-parent/docs/AGENT_HANDOFF.md`
  - Smoke test scripts (e.g. `.scratch/smoke-test.ps1`)
  - Any other files referencing the old IP (use `grep -r` to find them)
  Then commit and push the IP update.

## 8. Targeted Scope Testing Rule (代码修改后仅做定向范围测试铁律)
**STRICTLY FORBIDDEN**: Running full-suite regression tests (such as executing all 62 smoke test items) after localized code changes. Full-suite testing creates cross-service noise, generates unnecessary mock data, depletes server CPU / disk I/O, and slows down development.
**RULE**: Whenever code is modified, verification MUST strictly focus ONLY on the specific microservice, API endpoint, logic path, or UI page directly affected by the modification. Targeted verification must pass with zero errors before syncing back to local and committing.

