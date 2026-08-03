# 开发交接说明

## 开始前必读

1. `AGENTS.md`：产品方向与云端优先约束。
2. `PROJECT_STATUS.md`：当前完成度、边界与下一目标。
3. `docs/12-real-implementation-plan.md`：真实系统的分阶段顺序。
4. `docs/05-technical-architecture.md`、`docs/06-data-model.md`、`docs/07-api-contract.md`：技术、数据与接口契约。

## 仓库结构

| 路径 | 用途 | 当前状态 |
| --- | --- | --- |
| `src/App.jsx` | 应用装配与业务导航 | Gateway 模式连接机构、基础资料、长者、入住、护理、报表、通知和审计工作台。 |
| `src/system-overrides.css` | 应用扩展样式 | 包含基础资料、护理执行、机构目录、审计及既有设备中心/助手规则。 |
| `src/lib/api-client.js` | Gateway API 客户端基础 | 已实现令牌、幂等键、错误与 traceId 传递；所有业务领域客户端均使用该边界。 |
| `src/features/auth/` | 前端认证边界 | Gateway 登录、刷新、当前用户、机构目录与机构切换 API，使用内存会话保存令牌。 |
| `src/features/admissions/` | 入住调度前端边界 | 入住申请、评估、预留、确认、取消、调床、退住、清洁完成与审计 API；Gateway 页面已接入。 |
| `src/features/care/` | 护理执行前端边界 | 计划、模板、生成、任务、服务记录、异常跟进和交接 API；Gateway 工作台已完成表单与查询入口。 |
| `src/features/master-data/` | 基础资料前端边界 | 房间、床位、护理项目、评估类型、风险等级和班次目录的查询、维护与启停。 |
| `src/features/system/` | 机构与成员工作台 | 机构目录、成员、角色、菜单权限和数据范围的真实 RuoYi API 客户端及工作台。 |
| `src/lib/tenant-access.js` | 租户范围基础校验 | 用于测试和前端边界，不取代服务端鉴权。 |
| `backend/` | Java 21 养老领域服务骨架 | 包含 `care-service`、`device-service`、入住事务、护理计划/模板/任务、异常跟进、MySQL Flyway 与入住/护理幂等重放源码，待 RuoYi Cloud 接入和云端验证。 |
| `supabase/migrations/` | 可选 Postgres/Supabase 数据模型初稿 | 未执行，需先确认与 RuoYi JWT/RLS 的兼容方案。 |
| `.devcontainer/` | GitHub Codespaces 开发容器 | 配置 Node 22.16.0 和云端 `npm ci`。 |
| `.github/workflows/` | CI/CD 配置 | 未在 GitHub 运行；Vercel 依赖授权变量与 Secrets。 |
| `tests/` | 最小单元、集成测试骨架 | 需随真实领域服务与 Playwright 扩充。 |

## 已完成的源码能力

- 顶部导航 SaaS 工作台与独立登录/注册流程。
- 长者 360、入住床位、护理执行、基础资料、机构与权限、运营报表导出、站内通知和统一审计的服务端、API 客户端及工作台。
- 头像菜单中的个人中心、系统管理、成员角色、权限策略和可见机构切换。
- 设备概览/告警交互、右下角智护小助理、桌面与移动端布局。

源码闭环不代表已有真实认证、数据一致性、权限隔离、对象存储、设备连接、AI 调用或生产可用性；这些运行结论必须由云端验证日志证明。

## 第一位接手开发者的任务

1. 在云端配置 Nacos、Redis、核心数据库、对象存储与 RuoYi 基础服务；不要在本机安装。
2. 导入成员目录、Gateway 路由和机构目录 SQL，对护理库执行 Flyway V1-V19。
3. 在真实 RuoYi 平台验证登录、机构切换、成员权限和 Gateway 可信内部身份传递。
4. 在 MySQL 迁移后验证机构、基础资料、长者、入住、护理、报表导出、通知和审计：包括跨租户 `404`、成员停用 `403`、乐观锁 `409`、幂等重放、失败回滚和定时清理。
5. 在 Codespaces 或 Actions 完成 MySQL 并发、对象存储、浏览器 E2E 和移动端验证；仅在此后进入 EMQX 与真实设备联调。

## 云端配置清单

- GitHub Secrets：`VERCEL_TOKEN`、后端数据库凭据、OpenAI 服务端密钥、Supabase service-role key（如使用）。
- GitHub Variables：`ENABLE_VERCEL_DEPLOY`、`PRODUCTION_HEALTHCHECK_URL`。
- 环境变量格式：见 `.env.example`。浏览器只使用 `VITE_*` 公共变量；服务端密钥不得以 `VITE_` 开头。
- GitHub Environments：至少创建 `preview` 和带人工审批的 `production`。

## 不可跳过的检查

真实依赖安装后，必须在 Codespaces 或 GitHub Actions 执行 lint、类型检查、单元测试、集成测试、生产构建和 Playwright E2E。当前仓库的 Playwright 套件尚未引入，CI 的 readiness 标记不等同于 E2E 通过。

## 已知决策与待确认项

- 前端保持 React SaaS，不使用 RuoYi 默认 Vue 后台。
- RuoYi Cloud 管理身份、租户、权限、审计与网关；养老业务先集中在 `care-service`，设备集中在 `device-service`。
- `care-service` 当前以 `X-Platform-User-Id` 与 `X-Platform-Tenant-Id` 表达 Gateway 已验证的内部上下文。生产接入必须限制内部网络，并替换为 RuoYi Gateway 传递的签名内部声明；不得将这两个请求头直接暴露给浏览器。
- EMQX 负责设备 MQTT 连接；高频遥测与核心业务数据分库存储。
- 核心事务数据库确定为 MySQL 8；Supabase 仅作为可选私有附件对象存储。若未来使用 Supabase/Postgres 业务镜像，必须验证 RuoYi JWT 与 RLS 的安全模型后再执行迁移。
