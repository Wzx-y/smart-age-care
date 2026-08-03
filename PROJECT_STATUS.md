# 项目状态与接手说明

## 一句话简介

智护云 Care 是面向养老机构的多租户协同照护 SaaS：以 RuoYi Cloud 负责身份、租户和权限，以 Java 养老业务服务负责档案、床位、护理和设备闭环，以 React 提供顶部导航的现代 SaaS 工作台，并预留 EMQX 设备接入层。

## 当前进展

| 领域 | 状态 | 说明 |
| --- | --- | --- |
| 业务工作台 | 源码闭环完成，待云端验证 | React 已接入机构目录、成员/角色/数据范围、基础资料、长者 360、入住床位、护理执行、运营报表导出、通知和统一审计；Gateway 模式不以静态数据伪装接口结果。 |
| 视觉与响应式 | 已完成原型验收 | 顶部导航、桌面与移动端、助手最新回复自动可见均已在此前原型阶段验证。 |
| RuoYi Cloud | 源码已纳入，待云端验证 | 已导入官方 `springboot3` 基线，并新增 System 租户成员/机构目录、Gateway `/care/**` 租户上下文过滤和 Nacos 路由 SQL；尚未连接 MySQL/Nacos 或执行编译。 |
| Java 业务服务 | 业务源码闭环完成，待云端验证 | `care-service` 已具备长者档案、入住床位、护理、基础资料、运营指标、异步导出、站内通知、统一审计、保留清理和 Flyway V1-V19 源码及测试源码；尚未下载依赖、迁移或云端联调。`device-service` 仍为骨架。 |
| 业务数据库 | 未接入 | `care-service` 已有 MySQL Flyway 初稿；`supabase/migrations/` 保留为可选 Postgres/Supabase 方案，均未执行。 |
| 设备接入 | 未接入 | 已定义 EMQX、设备服务、时序数据与告警闭环方向。 |
| AI 助手 | 原型完成 | 当前为本地模拟答案；真实模型、知识库、审计与人工确认未接入。 |
| GitHub 与 CI/CD | 首个源码提交已推送，配置待验证 | 私有 GitHub 仓库已创建并推送首个源码提交。Codespaces 已预留 Node 22/Java 21；CI 覆盖前端与 Maven verify，但尚未执行。`.npm-cache` 清理提交仅在本机，因 Git HTTPS 网络失败尚未推送。 |

## 首要目标

先在 Codespaces 以版本化编排启动 MySQL 8、Redis 和 Nacos 3，导入 RuoYi/护理迁移并执行全量质量门禁；随后以真实平台验证“机构权限 -> 基础资料 -> 长者档案 -> 入住床位 -> 护理计划 -> 每日任务 -> 服务记录 -> 交接 -> 报表/通知/审计”的业务闭环。设备与 EMQX 真实接入、AI 服务端编排不属于当前业务端验收范围。

## 强制约束

- 不在本机安装、运行或调试 Node.js、Java、Maven、Docker、数据库、插件或依赖。
- 所有执行验证只在 GitHub Codespaces、GitHub Actions 或已授权云环境进行。
- 不提交密钥、个人数据、构建产物、数据库导出或 `.env` 文件。
- 没有云端证据时，不得声称迁移、测试、容器启动、部署或发布完成。

完整交接入口见 [NEXT_DEVELOPER_HANDOFF.md](NEXT_DEVELOPER_HANDOFF.md)，详细模块交接信息见 [docs/13-handoff.md](docs/13-handoff.md)。
