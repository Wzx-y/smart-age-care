# 发布验收报告

## 当前版本

- 版本：业务源码提交基线；私有 GitHub 仓库首个源码提交已于 2026-07-30 推送。
- 部署地址：未部署。
- 真实环境变量：未配置。

## 已完成功能

- SaaS 顶部导航、登录注册入口、长者档案、入住调度、护理执行、基础资料、运营报表、通知、统一审计、设备概览与告警处置工作台。
- 头像菜单下的个人中心、系统管理、租户上下文、成员角色与权限展示。
- 受控内容的智护小助理、思考状态、打字输出和移动端最新回复自动可见。
- 云端优先开发配置、环境变量模板与正式项目文档。
- 租户范围校验、RuoYi Gateway API 客户端、Supabase 核心数据迁移、最小单元与集成测试骨架，以及待授权的 GitHub CI/CD 工作流。
- Java 21 的 `care-service` 与 `device-service` 源码骨架、长者档案创建查询接口、MySQL Flyway V1-V19 和领域单元测试源码；`care-service` 已补充入住床位、护理执行、基础资料、实时运营指标、异步 CSV 导出、短时授权下载、站内通知、统一审计查询和最小审计源码。官方 RuoYi Cloud `springboot3` 基线及 System 成员目录、Gateway 租户上下文过滤、Nacos 路由 SQL 已纳入仓库；尚未下载依赖、编译、迁移或联调。

## 测试结果

- 本次未在本机安装或执行依赖、构建、测试、数据库迁移、容器启动或部署。
- GitHub 首个私有源码提交已推送。`.npm-cache` 清理提交仍在本机，因 Git 终端连接 `github.com:443` 失败而未推送。
- GitHub Codespaces、GitHub Actions、Vercel、Supabase、RuoYi、MySQL、Redis、Nacos 和 EMQX 云端验证：待执行。

## 人工授权步骤

从当前 GitHub `main` 创建 Codespace；完成 `.npm-cache` 清理提交；配置 MySQL、Redis、Nacos、EMQX、RuoYi、Supabase、OpenAI、Vercel 的云端变量与生产审批；执行文档 `09` 和 `10` 中的云端检查。

## 已知风险与下一阶段

业务工作台、Gateway API 客户端、RuoYi 扩展、`care-service`、迁移与测试源码已经纳入仓库；机构目录、基础资料、护理完整表单、报表导出、通知和审计不再存在源码功能缺口。当前尚未连接真实认证、数据库、对象存储，也未以真实 MySQL、Gateway 身份上下文或云端 CI 验证。设备 MQTT 接入、AI 服务端编排与端到端测试属于后续实现或云端验证边界。

完整实施顺序见 `docs/12-real-implementation-plan.md`，新接手开发者的入口与未决事项见 `PROJECT_STATUS.md` 和 `docs/13-handoff.md`。
