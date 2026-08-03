# GitHub 上传清单

本文用于将智护云 Care 源码提交到 GitHub 前的最终检查。它区分“可提交的源码状态”和“已在云端验证的运行状态”，避免将未执行的环境检查写成已完成。

## 提交范围

提交应包含：

- React/Vite 工作台及领域 API 客户端。
- `backend/care-service` 的领域服务、Flyway V1-V19、单元测试和运行说明。
- `backend/ruoyi-cloud` 的 Gateway 租户上下文扩展、System 成员/机构目录扩展及 `sql/smart-age-care` SQL。
- `docs/` 中的产品、架构、数据模型、API、安全、测试、部署、完成定义和当前状态说明。
- `.github/workflows/`、`.devcontainer/`、`.env.example`、`.nvmrc`、`package-lock.json` 和 Maven 配置。

禁止提交：

- `.env`、密钥、令牌、密码、真实长者资料、数据库导出、签名下载地址或云端日志中的敏感字段。
- `node_modules/`、`dist/`、`.npm-cache/`、覆盖率、Playwright 报告、截图以外的测试产物。

## 提交前静态检查

在不改变本机环境的前提下，确认以下内容已进入工作区：

1. `git diff --check` 无空白错误。
2. 所有前端业务客户端均使用 `/care/api/v1/**` 进入 Gateway，浏览器请求不携带 `tenantId`、操作者或审计动作。
3. 机构、成员、角色、数据范围、基础资料、长者、入住、护理、报表、通知和审计均有 API、服务端实现与文档入口。
4. `.gitignore` 覆盖依赖、构建产物、环境变量和测试报告。
5. `README.md`、`docs/16-business-completion-acceptance.md` 与 `docs/17-current-source-status.md` 对源码范围和云端验证状态没有互相矛盾的表述。
6. RuoYi 的 JWT、Gateway 内部密钥、Nacos 数据库和认证密钥均未写入源码，且已在 GitHub Secrets 或运行环境中配置。

## 云端必做验证

从 GitHub Codespaces 或 GitHub Actions 执行，保留每一步的日志和版本号：

```bash
npm ci
npm run lint
npm run typecheck
npm run test
npm run build
mvn --batch-mode --file backend/pom.xml verify
mvn --batch-mode --file backend/ruoyi-cloud/pom.xml verify
```

随后导入 RuoYi SQL（成员目录、Gateway 路由、机构目录），对 `care-service` 执行 Flyway V1-V19，启动 MySQL、Redis、Nacos、RuoYi Auth/System/Gateway 与 `care-service`。必须验证：

- 登录、当前用户、机构列表和机构切换后的真实数据刷新。
- 基础资料、长者 360、入住申请、预留、确认、取消、退住、清洁完成和预留超时释放。
- 护理计划、模板、任务、服务记录、异常跟进和交接。
- 报表聚合、异步导出、私有文件授权下载及过期拒绝。
- 通知已读、审计筛选、审计保留清理、附件直传/预览/下载。
- 跨租户 `404`、成员停用后 `403`、乐观锁 `409`、幂等重放和事务回滚。

## 上传后的状态维护

首次上传后，不得把 CI 配置、测试源码或未执行的迁移描述为“已通过”。只有将 GitHub Actions/Codespaces 的 URL、提交 SHA、迁移版本和验证日期写入 `docs/11-release-acceptance-report.md` 后，才可声明对应云端项已验证。
