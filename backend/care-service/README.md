# care-service

养老核心业务服务，覆盖长者档案、入住床位、护理执行、基础资料、运营报表、异步导出、站内通知和统一审计。所有领域资源均按可信租户上下文隔离，写操作使用状态机、事务、幂等键或乐观锁，具体契约见仓库根目录 `docs/07-api-contract.md`。

## 核心 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/residents` | 按当前租户列出未归档长者；可传 `keyword`。 |
| GET | `/api/v1/residents/{id}` | 读取当前租户内的长者。 |
| POST | `/api/v1/residents` | 创建待入住长者档案。 |
| `/api/v1/admissions` | 入住申请、评估、预留、确认、取消、调床、退住和审计。 |
| `/api/v1/care-plans` | 护理计划、模板、每日生成、任务、服务记录、异常跟进和交接。 |
| `/api/v1/notifications` | 站内通知读取与已读。 |
| `/api/v1/audit-events` | 统一审计查询。 |

所有成功响应使用 `{ "code": 0, "message": "ok", "data": ... }`，失败响应保留 HTTP 状态和相同 JSON 形状。资源不属于当前租户时统一返回 404。

## 基础资料 API

`/api/v1/master-data` 提供房间、床位和护理项目、评估类型、风险等级、班次目录的查询、创建、版本化编辑与启停。所有接口均要求当前成员拥有 `care:master-data:manage`；房间与床位不物理删除，停用前由服务端校验床位状态和在院关联，并写入最小化审计记录。入住床位查询和状态机只使用启用房间下的启用床位。

## 运营报表与导出 API

`GET /api/v1/operational-reports` 返回当前机构的实时运营指标；`/api/v1/exports` 负责异步创建和查询机构级 CSV 导出任务，`POST /api/v1/exports/{id}/access-url` 仅为未过期完成任务签发 5 分钟私有下载地址。响应不包含对象键或存储密钥，角色权限分别为 `care:report:view`、`care:export:manage` 和 `care:export:download`。

## 身份边界

服务只接受 RuoYi Gateway 已认证、已注入的 `X-Platform-User-Id` 和 `X-Platform-Tenant-Id` 内部上下文。部署时必须限制为内部网络，并改为签名内部声明或 mTLS，不得让浏览器直接访问该服务或自行构造请求头。

## 云端运行前置条件

- Java 21、Maven、MySQL 8。
- `CARE_DB_URL`、`CARE_DB_USERNAME`、`CARE_DB_PASSWORD` 仅在云端环境配置。
- Flyway 会在受控环境中执行 `db/migration/V1__create_care_core.sql` 至 `V19__add_notifications_and_unified_audit.sql`；首次执行前须由数据库管理员审核。

本机不执行 Maven、迁移或服务启动。
