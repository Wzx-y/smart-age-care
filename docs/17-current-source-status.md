# 当前源码状态

本文件是本次业务端补齐后的状态说明。设备真实接入和 AI 服务不纳入本阶段。

## 已补齐源码

- 业务客户端与 Gateway 的 `/care/api/v1/**` 路由前缀。
- 房间、床位、护理项目、评估类型、风险等级和班次的真实维护工作台。
- 护理计划、模板、任务开始/完成/异常、服务记录筛选、异常跟进和班次交接工作台。
- 入住、长者、护理和报表数据驱动的机构总览。
- 统一审计查询、筛选和当前结果导出工作台。
- 长者档案操作审计以及入住创建、取消、退住、预留超时释放通知。
- RuoYi System 机构目录元数据 API 和机构切换成员资格校验。

## 不得提前声称的事项

本地没有运行 Maven、Node 构建、数据库、服务或容器。没有 GitHub Actions/Codespaces 运行日志前，不得声称 Flyway、前后端联调、跨租户隔离、附件存储、导出任务、E2E 或部署成功。

源码已提供完整容器化 Codespaces 编排、无密钥变量模板、启动脚本和静态检查，但尚未在 Codespaces 执行；它们不能替代服务启动日志、登录结果、SQL/Flyway 记录或端到端验收证据。

## 上传前云端清单

1. 执行 `backend/ruoyi-cloud/sql/smart-age-care/001_tenant_member_directory.sql`、`002_gateway_care_route.sql` 和 `003_tenant_directory.sql`。
2. 启动 MySQL、Redis、Nacos、RuoYi Auth/System/Gateway、`care-service`。
3. 执行 `npm ci`、`npm run lint`、`npm run typecheck`、`npm test`、`npm run build`、`mvn --file backend/pom.xml verify` 和 `mvn --file backend/ruoyi-cloud/pom.xml verify`。
4. 执行登录、租户切换、基础资料、长者、入住、护理、报表、通知、审计和附件 Playwright 流程。
5. 记录失败重试、跨租户拒绝、对象存储签名过期、导出过期、定时任务和数据库回滚证据。
