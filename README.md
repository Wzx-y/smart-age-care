# 智护云 Care

智护云 Care 是面向养老机构的多租户协同照护 SaaS。平台以 RuoYi Cloud 提供认证、租户、成员、角色、权限、审计和网关能力，以 React/Vite 提供现代化运营工作台，以养老领域服务承载入住、护理和设备业务，连接机构日常照护与运营决策。

RuoYi 认证、Gateway 租户上下文、`care-service` 业务数据库迁移和前端 Gateway 客户端均已纳入源码。真实 MySQL、Redis、Nacos 与 RuoYi 实例由 GitHub Codespaces 启动并完成联调验收；在获得云端运行记录前，不将其描述为已部署或已验证。

## 核心能力

- **机构与权限**：机构租户目录、成员角色、模块权限与数据范围统一由 RuoYi 体系管理；机构切换经成员资格校验，业务请求经 Gateway 传递可信身份与租户上下文。
- **长者 360 档案**：集中呈现基本信息、照护信息、风险信号、服务记录和在院状态，支撑长期连续照护。
- **入住与床位调度**：从入住评估、候选长者、空闲床位到确认入住形成闭环；床位预留与确认使用版本校验、事务和幂等控制保障并发安全。
- **护理计划与执行**：围绕“评估 - 计划 - 每日任务 - 服务记录 - 异常跟进 - 班次交接”建立可追溯的护理执行链路。
- **基础资料与运营**：房间、床位、护理项目、评估类型、风险等级和班次目录受控维护；运营首页汇总入住、长者、护理和报表数据，支持异步导出与短时授权下载。
- **通知与审计**：入住、退住、预留超时、护理异常、交接和逾期任务触发站内通知；档案、护理、基础资料和导出操作进入统一审计查询与保留清理策略。
- **设备与告警运维**：设备中心覆盖资产、状态、使用、巡检和维护信息；告警处置围绕确认、派单、维修和复检组织运维闭环。
- **运营与智能协作**：提供经营视图、受控导出和照护助手入口；助手只提供系统操作与养老知识指引，不替代医疗判断、应急处置或机构制度。

## 技术架构

```text
React / Vite SaaS 工作台
        |
RuoYi Gateway / Auth / System
        |
care-service -------------------- MySQL
        |
device-service ------------------ EMQX / IoT
        |
AI、附件与导出适配器
```

- **前端**：React 19、Vite、Lucide，适配桌面和移动端；生产业务入口使用受 Gateway 保护的 API、真实机构上下文与服务端数据。
- **平台底座**：RuoYi Cloud `springboot3`，负责认证、用户、租户、角色、权限、字典、审计与服务网关。
- **领域服务**：Java 21、Spring Boot 3、Spring Cloud Alibaba、JDBC、Flyway；护理服务覆盖长者、入住、床位、护理计划、任务、服务记录、异常和交接。
- **数据与安全**：MySQL 8 作为核心业务数据存储；业务资源以 `tenant_id` 隔离，写操作使用幂等键、乐观锁与审计记录。
- **设备接入**：通过 EMQX/MQTT 与厂商协议适配器接入遥测、设备状态、告警和工单能力。

## 业务闭环

```text
长者评估 -> 入住申请 -> 床位预留 -> 确认入住
    -> 护理计划 -> 每日任务 -> 服务记录 -> 异常跟进 -> 班次交接

设备接入 -> 状态监测 -> 告警确认 -> 派单维修 -> 复检归档
```

## 项目结构

| 路径 | 说明 |
| --- | --- |
| `src/` | React SaaS 工作台、认证、入住、护理和通用 API 客户端 |
| `backend/ruoyi-cloud/` | RuoYi Cloud 平台基线、租户成员目录与 Gateway 配置 |
| `backend/care-service/` | 长者、入住床位、护理计划、任务、交接和审计领域服务 |
| `backend/device-service/` | 设备遥测标准化、厂商适配器和告警领域服务 |
| `supabase/` | 可选的附件存储与 PostgreSQL/Supabase 数据迁移方案 |
| `docs/` | 产品、架构、数据模型、接口、安全、测试、部署与交接文档 |
| `.github/workflows/` | 持续集成、预览与生产发布工作流 |

## 云端开发

本项目在 GitHub Codespaces、GitHub Actions 或经授权的云环境中完成依赖安装、构建、迁移、测试与部署。开发容器使用 Node.js `22.16.0`、Java 21 与 Maven。

```bash
npm run lint
npm run typecheck
npm run test
npm run build
mvn --batch-mode --file backend/pom.xml verify
mvn --batch-mode --file backend/ruoyi-cloud/pom.xml verify
```

不要提交 `.env`、令牌、密码、个人数据、数据库导出、`node_modules/`、`dist/` 或测试产物。环境变量格式见 [`.env.example`](.env.example)，真实密钥仅配置在 GitHub Secrets 或服务端运行环境中。

Codespaces 集成环境以 [infra/codespaces/compose.yaml](infra/codespaces/compose.yaml) 启动 MySQL、Redis 和 Nacos；随后用 `bash scripts/codespaces/start-services.sh` 启动业务服务与前端。完整初始化和验证顺序见[部署运行手册](docs/10-deployment-runbook.md)。

也可使用 `docker-compose.codespaces.yml` 作为全容器化联调入口：它在 Docker 网络中启动 MySQL、Redis、Nacos、RuoYi Auth/System/Gateway、`care-service` 与 Vite。先复制 `.env.codespaces.example` 为被忽略的 `.env.codespaces` 并在 Codespaces Secrets 提供变量，再运行 `bash scripts/start-codespaces-stack.sh`，最后在 Ports 面板打开 `5173`。同一 Codespace 只能选择这一入口或 `scripts/codespaces/` 入口之一，不能同时运行两套编排。

## GitHub 上传与验收

仓库可作为源码提交进入 GitHub：前端、`care-service`、RuoYi System/Gateway 扩展、Flyway 迁移、SQL、测试源码、CI 和运行文档均已纳入版本控制。上传前按 [GitHub 上传清单](docs/18-github-upload-checklist.md) 检查敏感文件、提交范围和云端验证入口。

“源码闭环完成”不等于“云端联调或生产发布完成”。真实 MySQL 迁移、对象存储、RuoYi/Gateway 联调、跨租户验证及 E2E 必须由 Codespaces 或 GitHub Actions 的运行日志证明，详见[当前源码状态](docs/17-current-source-status.md)。

## 文档

- [产品简述](docs/00-product-brief.md)
- [需求说明](docs/01-prd.md)
- [技术架构](docs/05-technical-architecture.md)
- [数据模型](docs/06-data-model.md)
- [API 契约](docs/07-api-contract.md)
- [安全与隐私](docs/08-security-and-privacy.md)
- [测试计划](docs/09-test-plan.md)
- [部署运行手册](docs/10-deployment-runbook.md)
- [RuoYi Cloud 集成说明](docs/15-ruoyi-cloud-integration.md)
- [业务端完成定义与验收清单](docs/16-business-completion-acceptance.md)
- [当前源码状态](docs/17-current-source-status.md)
- [GitHub 上传清单](docs/18-github-upload-checklist.md)

参与开发前请先阅读 [AGENTS.md](AGENTS.md) 与 [NEXT_DEVELOPER_HANDOFF.md](NEXT_DEVELOPER_HANDOFF.md)。
