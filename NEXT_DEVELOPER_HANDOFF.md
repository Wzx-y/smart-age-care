# 智护云 Care：开发交接总览

## 当前结论

这是养老机构多租户协同照护 SaaS。React/Vite 提供顶部导航工作台；RuoYi Cloud 规划负责认证、租户、成员、权限、审计和网关；Java `care-service` 承担养老业务事务；`device-service` 和 EMQX 是后续设备接入方向。

仓库包含完整的业务源码和用于产品展示的前端交互。官方 RuoYi Cloud `springboot3` 基线已纳入 `backend/ruoyi-cloud`，并完成租户成员/机构目录、Gateway 租户上下文过滤和护理服务权限校验源码。机构、基础资料、长者、入住、护理、报表导出、通知和审计均已有前后端入口；没有在本机或云端执行依赖安装、编译、测试、Flyway 迁移、容器启动、部署或发布。不得将当前状态描述为已验证、已上线或已部署。

## 强制约束

- 不在本机安装或运行 Node.js、Java、Maven、Docker、数据库、插件或依赖。
- 构建、测试、迁移、部署只能在 GitHub Codespaces、GitHub Actions 或授权云环境执行。
- 不提交 `.env`、令牌、密码、个人数据、数据库导出、`node_modules/`、`dist/` 或测试产物。
- 真实密钥只放 GitHub Secrets、Vercel、Supabase 或服务端环境变量；格式见 `.env.example`。
- 每次修改同步更新测试源码和相关文档；没有云端记录前，不能声称任何检查成功。

## 产品约束

- 保持简约科技感 SaaS。业务主导航必须在顶部，禁止加入常驻左侧导航。
- 登录/注册为独立入口；个人中心、系统管理、退出登录在头像菜单。
- 演示模式保留示例数据；Gateway 模式逐模块使用真实 API。
- 智护助手只提供系统操作和审核后的养老知识，不展示模型思维链，不替代医疗判断、应急处置或机构制度。

视觉与交互的完整规则以 `AGENTS.md` 为准。

## 当前完成度

| 领域 | 已有源码 | 仍待完成或验证 |
| --- | --- | --- |
| 工作台 | 登录、长者、入住、护理、基础资料、报表、通知、审计、机构权限、设备展示、桌面/移动交互 | 真实认证、数据和生产可用性验证 |
| React Gateway | 登录、会话、机构切换、机构/成员/角色/数据范围、基础资料、长者、入住、护理、报表、通知和审计 | 真实 RuoYi 联调与浏览器 E2E |
| 入住 | 申请、评估、床位、预留、确认、取消、调床、退住、清洁、审计、乐观锁、幂等、Flyway | MySQL 并发、回滚、定时释放验证 |
| 护理 | 计划、模板、任务、服务记录、异常跟进/结案、幂等、按日生成、班次交接、成员权限校验、Flyway | 机构时区和云端验证 |
| RuoYi Cloud | 官方 `springboot3` 基线、System 租户成员目录、Gateway `/care/**` 租户上下文过滤、护理权限码 | MySQL/Nacos 实例、菜单和角色数据、真实登录与网关联调 |
| 设备与 IoT | `device-service` 骨架、遥测标准化、告警状态 | EMQX、设备身份、MQTT ACL、影子、工单、实时推送、真实设备 |
| AI、导出、通知与审计 | AI 仍为预设回答；导出、通知和统一审计已有服务端、Gateway 适配器和工作台源码 | AI 服务端编排/知识库/人工确认；导出、通知、审计需在 Codespaces 验证对象存储、定时扫描、权限和保留清理 |
| CI/CD | Codespaces、PR、预览、生产工作流配置 | GitHub 授权、实际 CI、健康检查、部署和审批记录 |

## 架构与安全边界

```text
React SaaS -> RuoYi Gateway/Auth/System -> care-service -> MySQL
                                          -> device-service -> EMQX / IoT
                                          -> AI / export adapters (后续)
```

浏览器只可在访问 `/care/**` 时提交所选 `X-Tenant-Id`，不得提交任何内部身份头。RuoYi Gateway 在令牌认证后调用 System 内部成员目录，清除浏览器伪造的内部头，再向下游写入 `X-Platform-User-Id`、`X-Platform-Tenant-Id` 和仅服务端可用的内部密钥。`care-service` 必须校验内部密钥，并在每次护理写操作前重新校验成员资格和权限；这些源码均未在云端联调。

## 重要目录

| 路径 | 作用 |
| --- | --- |
| `src/App.jsx` | 原型、演示/Gateway 模式切换 |
| `src/features/auth/` | Gateway 登录、刷新、用户、租户与会话 |
| `src/features/admissions/` | 入住调度 API |
| `src/features/care/` | 护理 API，写请求带幂等键 |
| `backend/care-service/` | Java 21 养老事务、JDBC、Flyway、JUnit 源码 |
| `backend/device-service/` | 设备服务起点 |
| `backend/ruoyi-cloud/` | 官方 RuoYi Cloud 基线、System 成员目录和 Gateway 租户过滤 |
| `docs/` | 产品、架构、接口、安全、测试、部署、验收 |
| `.devcontainer/` | Codespaces：Node 22.16.0、Java 21、Maven |
| `.github/workflows/` | CI、预览、生产工作流，尚未运行 |

## 护理服务现有契约

- 计划：`/api/v1/care-plans`，创建草稿、发布、列表。
- 任务：`/api/v1/care-tasks`，创建、列表、完成服务记录、异常。
- 异常：`/api/v1/care-task-follow-ups`，列表、结案。
- 模板与生成：`/api/v1/care-plan-task-templates`、`/api/v1/care-task-generation-runs`。
- 全部护理写操作需要 `Idempotency-Key`；状态/版本冲突返回 `409`；跨租户资源返回不可枚举的 `404`。
- 每日生成以“租户 + 模板 + 服务日期”唯一约束去重。当前按 UTC 生成，接入 RuoYi 租户资料后必须改为机构时区。

请求体、状态和错误码以 `docs/07-api-contract.md` 为准；数据表和索引见 `docs/06-data-model.md`。

## 推荐继续顺序

1. 新增版本化的 Codespaces Docker Compose 编排，并为开发容器启用 Docker-in-Docker：启动 MySQL 8、Redis、Nacos 3 和 EMQX，不复用上游 Nacos 的硬编码密码或关闭认证配置。
2. 在 GitHub Codespaces Secrets 配置数据库、Nacos、EMQX 和内部服务密钥；重建容器后记录四项基础服务的健康检查证据。
3. 导入官方 RuoYi SQL、租户成员目录 SQL 和 Gateway 路由 SQL；对护理库执行 Flyway V1-V8。
4. 分别构建并启动 RuoYi Cloud Maven 根工程和 `backend/pom.xml`，验证登录、Gateway 路由、租户切换、权限拒绝、交接阻断与幂等重放。
5. 在云端执行机构、基础资料、长者、入住、护理、报表导出、通知和审计的跨租户集成与浏览器 E2E，记录结果到发布验收报告。
6. 完成登录、入住、护理、权限隔离和桌面/移动端的 Playwright E2E，再进入设备和 EMQX 真实设备联调。

## 云端首次执行清单

1. 私有 GitHub 仓库和首个源码提交已创建并推送；尚无 Codespaces、Actions 或部署运行记录。
2. 先从最新 GitHub `main` 创建 Codespace，完成 `.npm-cache` 清理提交并推送。当前本机存在该清理提交但 Git HTTPS 连接失败，状态为 `ahead 1`。
3. 在 GitHub Secrets/Variables 配置数据库、Nacos、EMQX、RuoYi、Vercel、Supabase、OpenAI 等实际变量；禁止写入仓库。
4. 创建 `preview` 与需人工审批的 `production` Environment。
5. 在 Codespaces 运行基础设施、lint、类型检查、前端单元/集成测试、两套 Maven verify、生产构建和 Playwright。
6. 仅在检查记录齐全后执行 Flyway、预览部署、健康检查与生产审批。

变量、部署、排错和回滚步骤见 `docs/10-deployment-runbook.md`。当前没有部署地址。

## 建议阅读顺序

1. `AGENTS.md`
2. `PROJECT_STATUS.md`
3. 本文档
4. `docs/12-real-implementation-plan.md`
5. `docs/05-technical-architecture.md`
6. `docs/06-data-model.md`
7. `docs/07-api-contract.md`
8. `docs/08-security-and-privacy.md`
9. `docs/09-test-plan.md`
10. `docs/11-release-acceptance-report.md`

## 交接事实

- 当前未部署，无部署 URL。
- 当前未配置真实环境变量，工作区不包含真实密钥。
- 私有 GitHub 仓库已存在且包含首个源码提交；当前本机缓存清理提交未推送，原因是 Git 终端无法连接 `github.com:443`，不是代码或身份验证错误。
- 当前未执行本地或云端构建、测试、数据库迁移、容器启动或部署。
- 下一个开发窗口应以“源码已推送，待 Codespaces 基础设施编排与云端验证”为起点。
