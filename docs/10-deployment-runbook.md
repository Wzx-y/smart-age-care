# 部署手册

## 首次授权

1. 私有 GitHub 仓库已创建，首个源码提交已推送；从 GitHub `main` 创建 Codespace。当前本机缓存清理提交未推送，不应将其视为远程已完成。
2. 在 Codespaces Secrets 配置 MySQL、Nacos、EMQX、RuoYi 内部服务密钥和至少 64 位的 `RUOYI_JWT_SECRET`，以及附件和导出私有桶的 `CARE_ATTACHMENT_STORAGE_*`、`CARE_EXPORT_STORAGE_*` 变量；Nacos 必须同时配置 `NACOS_DB_USERNAME`、`NACOS_DB_PASSWORD`、`NACOS_AUTH_TOKEN_SECRET`、`NACOS_SERVER_IDENTITY_KEY` 与 `NACOS_SERVER_IDENTITY_VALUE`；配置 `NOTIFICATION_OVERDUE_SCAN_MS`、`AUDIT_RETENTION_DAYS` 与 `AUDIT_RETENTION_SCAN_MS`；授权 GitHub Actions、Vercel 项目、Supabase 项目及 RuoYi 部署环境。
3. 使用版本化 Docker Compose 编排启动 MySQL 8、Redis、Nacos 3 和 EMQX。`backend/ruoyi-cloud/docker/nacos/conf/application.properties` 不再提供数据库口令或 Nacos 认证密钥默认值；所有值只能从云端 Secrets 注入，认证不得关闭。
4. 在对应云端环境配置 `.env.example` 所列真实变量，不上传 `.env`。
5. 为生产部署环境设置人工审批保护规则。

## Codespaces 集成环境

1. 创建或重启 Codespace 前，在其 Secrets 中设置 `.env.example` 的数据库、Nacos、RuoYi 内部密钥变量，另设 `MYSQL_ROOT_PASSWORD`、`RUOYI_DB_USERNAME` 和 `RUOYI_DB_PASSWORD`。数据库口令应使用十六进制随机值，避免初始化脚本的 SQL 转义歧义。
2. 容器初始化后执行 `bash scripts/codespaces/infra-up.sh`。该命令仅启动 MySQL、Redis 和 Nacos，并以本机回环端口暴露给 Codespace 内的 Java 进程；不得将这些端口改为公开访问。首次执行会以 `NACOS_PASSWORD` 初始化 Nacos 管理员，不保留上游 SQL 的默认管理员。
3. 执行 `bash scripts/codespaces/start-services.sh` 启动 System、Auth、`care-service`、Gateway 与 Vite。前端使用 `/gateway` 的 Vite 反向代理访问本机 Gateway，避免把临时 Codespaces URL 编译进浏览器包。
4. 服务启动完成后执行 `bash scripts/codespaces/verify.sh`。该脚本包含健康检查、前端静态检查、前端测试、构建及两套 Maven `verify`，只能在 Codespaces 或其他授权云环境运行。
5. MySQL 首次初始化会导入 RuoYi 官方 SQL、租户目录 SQL、成员目录 SQL 和护理 Gateway 路由；`care-service` 首次启动负责 Flyway V1-V19。清理 Docker 卷会删除测试数据，重新初始化前应先保留所需的非敏感验证记录。

## Codespaces 全容器化联调

`docker-compose.codespaces.yml` 与 `scripts/start-codespaces-stack.sh` 是另一条仅限 Codespaces 的完整容器化联调入口。它启动 MySQL 8、Redis、Nacos、RuoYi Auth/System/Gateway、`care-service` 和 Vite；浏览器只打开 `5173`，Vite 在容器网络内代理 `/gateway` 到 Gateway。

1. 执行 **Codespaces: Rebuild Container**，使 Docker-in-Docker 生效。
2. 从 `.env.codespaces.example` 创建被忽略的 `.env.codespaces`，只在 Codespaces Secrets 或该本地文件中提供数据库、Nacos、JWT 和内部服务密钥。
3. 运行 `bash scripts/start-codespaces-stack.sh`。首次运行只向空 Docker 数据卷导入跟踪的 SQL，并由 `care-service` 执行 Flyway；脚本会等待 MySQL 根密码认证查询成功后再导入，避免容器首次初始化时过早执行 SQL；不会删除或重置已有卷。
4. 在 Ports 面板打开 `5173`，并用 `docker compose --env-file .env.codespaces -f docker-compose.codespaces.yml logs -f` 查看日志。

不要与 `scripts/codespaces/infra-up.sh` 和 `scripts/codespaces/start-services.sh` 同时使用；两条入口会争用同一 Codespace 的服务端口和数据库资源。

## 部署

1. 在 Codespaces 验证四项基础服务健康，导入官方 RuoYi SQL、成员目录 SQL、Gateway 路由 SQL，并对护理库执行 Flyway V1-V19；验证通知逾期扫描、成员级广播已读状态、统一审计查询与保留清理；在私有 Supabase Storage 桶中完成附件签名直传、导出 CSV 写入/签名下载和对象生命周期清理烟测，不记录签名地址或密钥。
2. 分别构建 RuoYi Cloud Maven 根工程与 `backend/pom.xml`，再执行前端依赖安装、质量检查和构建。
3. Pull Request 触发 CI 与预览部署；预览完成后运行烟测。
4. 合并到 main 后触发生产候选部署，审批人确认后发布。
5. 健康检查通过后记录版本、地址、变更和回滚点。

## 回滚与排错

健康检查失败、租户隔离异常、认证错误或数据库迁移失败时，停止流量切换，回滚到上个已验证版本，保留 traceId 和部署日志。不要通过删除生产数据解决故障；迁移回滚必须单独评审。
