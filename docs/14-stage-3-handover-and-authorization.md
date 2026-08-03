# 阶段 3 更新：班次交接与 RuoYi 成员权限校验

状态：源码已完成，待 GitHub Codespaces 或 GitHub Actions 在真实 RuoYi、MySQL 环境执行验证。本机未安装或运行 Java、Maven、数据库、迁移、构建或测试。

## 架构

`care-service` 的 `CareAuthorizationService` 以当前 Gateway 身份上下文中的 `tenantId` 与 `userId` 为唯一鉴权输入，调用 `RuoYiTenantMemberDirectory` 获取当前租户的成员状态和权限。成员目录调用约定：

`GET {RUOYI_MEMBER_DIRECTORY_URL}/internal/v1/tenant-members/{userId}?tenantId={tenantId}`

最小响应字段为 `tenantId`、`userId`、`active` 和 `permissions`。响应中的租户或成员不匹配、成员停用、权限缺失均拒绝写入并返回 `403`；配置缺失、网络失败或上游异常返回 `503` 并拒绝写入。生产部署必须使该接口仅能从受保护的内网服务间访问。

当前护理写入权限码：

- `care:plan:manage`：创建计划、维护任务模板。
- `care:plan:publish`：发布护理计划。
- `care:task:manage`：创建和按日生成任务；新任务或模板的执行人还必须具备 `care:task:execute`。
- `care:task:execute`：完成任务或标记异常；当前操作者还必须等于任务的 `assigneeId`。
- `care:follow-up:manage`：结案异常跟进项。
- `care:handover:manage`：创建和提交班次交接。
- `care:handover:receive`：作为接班人被指定到交接单。
- `care:master-data:manage`：维护房间、床位、护理项目、评估类型、风险等级和班次基础资料。
- `care:report:view`：查看当前机构运营汇总。
- `care:export:manage`：创建和查询当前机构导出任务。
- `care:export:download`：为已完成且未过期导出任务获取短时下载地址。
- `care:notification:view`：读取当前机构可见通知并标记已读。
- `care:audit:view`：查询当前机构最小化统一审计事件。

## 数据模型

Flyway `V8__add_care_shift_handover.sql` 新增以下表：

| 表 | 作用 | 关键约束 |
| --- | --- | --- |
| `care_shift_handover` | 交接日期、班次、交班人、接班人、说明、状态、时间戳、版本 | `(tenant_id, shift_date, shift_code, from_user_id)` 唯一；按租户、状态、日期索引 |
| `care_shift_handover_item` | 交接单与护理任务的关联 | `(handover_id, task_id)` 唯一；按租户、任务索引 |

交接单只有 `DRAFT` 与 `SUBMITTED` 两种状态。表中仅保留任务 ID 和必要交接说明，不复制护理服务记录、异常详情或长者个人资料。查询、更新、关联读取均强制含 `tenant_id`。

## API 合同

所有写接口要求 `Idempotency-Key`，并由现有 `care_write_idempotency` 保存请求 SHA-256 指纹和结果资源定位信息。

| 接口 | 请求体 | 业务规则 |
| --- | --- | --- |
| `GET /api/v1/care-shift-handovers` | 无 | 仅返回当前租户交接单和任务 ID。 |
| `POST /api/v1/care-shift-handovers` | `shiftDate`, `shiftCode`, `toUserId`, `note`, `taskIds` | 当前用户为交班人；接班人须为有效成员并有接班权限；任务必须属于当前租户且状态为 `COMPLETED` 或 `EXCEPTION`。 |
| `POST /api/v1/care-shift-handovers/{handoverId}/submit` | `handoverVersion` | 仅草稿和当前版本可提交；关联任务存在任意 `OPEN` 异常跟进项即返回 `409`。 |

错误语义：无权限 `403`，成员目录不可用 `503`，跨租户或不存在资源 `404`，状态/版本/重复班次/开放异常冲突 `409`，请求字段不合法 `400`。

## 安全和隐私

`RUOYI_MEMBER_DIRECTORY_URL` 是服务端变量，仅可在 GitHub Secrets、Codespaces、部署平台或受控运行时配置；不得进入 `.env`、`VITE_*` 变量、前端包或仓库。成员目录故障时采用拒绝写入策略，避免故障降级造成越权。

班次交接不能绕过异常闭环：只允许已完成或异常任务加入，且提交前检查开放跟进项。幂等表不保存请求正文或护理资料，仅保留指纹、资源 ID、状态和版本。

## 测试源码和云端验证

新增 `CareAuthorizationServiceTest` 与 `CareShiftHandoverServiceTest`，覆盖权限委托、非执行人拒绝、交接任务状态、当前交班人、开放异常阻断和提交版本递增。这些测试源码尚未执行。

在云端必须增加并执行：

1. 使用 WireMock 或等效模拟 RuoYi 成员目录的成功、停用、租户不匹配、无权限、超时与 5xx 响应。
2. 在 MySQL 执行 V1 至 V8，验证并发创建、幂等重放、事务回滚、跨租户不可枚举和开放异常结案后的提交重试。
3. 在 Gateway、成员目录与 MySQL 就绪后，完成交接创建、异常阻断、结案、提交和桌面/移动端权限隔离的端到端测试。

## 计划同步

阶段 3 的“班次交接”“RuoYi 成员权限校验”和护理 Gateway 工作台均已完成源码收口。工作台包含计划、模板、任务开始/完成/异常、服务记录筛选、异常结案和交接操作，并从真实成员目录选择人员。实际部署、数据库迁移、测试结果、版本和地址仍必须在 `docs/11-release-acceptance-report.md` 取得云端证据后填写。
