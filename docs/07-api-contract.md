# API 契约

## 通知与统一审计

| 方法 | 路径 | 规则 |
| --- | --- | --- |
| GET | `/api/v1/notifications?unreadOnly=false` | 返回当前成员可见的个人与机构广播通知，要求 `care:notification:view`。 |
| POST | `/api/v1/notifications/{notificationId}/read` | 仅将当前成员可见且未读的通知标为已读。 |
| POST | `/api/v1/notifications/read-all` | 将当前成员可见的未读通知全部标为已读。 |
| GET | `/api/v1/audit-events?resourceType=&action=&actorId=&limit=50` | 合并当前机构入住、护理、基础资料和导出最小审计，要求 `care:audit:view`，`limit` 最大 100。 |

浏览器不得传递 `tenantId`、接收成员、通知分类/优先级、审计动作或资源归属；通知不存在、跨租户或不可见时返回 `404`。通知和审计响应不得包含长者身份资料、异常说明、交接正文、附件对象键、签名地址或服务密钥。

## 运营报表与导出

| 方法 | 路径 | 规则 |
| --- | --- | --- |
| GET | `/api/v1/operational-reports?periodStart=YYYY-MM-DD&periodEnd=YYYY-MM-DD` | 返回当前机构的实时运营汇总，要求 `care:report:view`。 |
| GET | `/api/v1/exports` | 返回当前机构导出任务，不返回私有对象键，要求 `care:export:manage`。 |
| POST | `/api/v1/exports` | 创建 `OPERATIONAL_SUMMARY_CSV` 异步任务，请求体包含 `exportType`、`periodStart`、`periodEnd`。 |
| POST | `/api/v1/exports/{exportId}/access-url` | 仅为未过期 `READY` 任务签发 5 分钟下载地址，要求 `care:export:download`。 |

导出状态为 `QUEUED`、`GENERATING`、`READY`、`FAILED`、`EXPIRED`。浏览器不得传递 `tenantId`、`createdBy`、存储对象键或过期时间；资源不存在或跨租户返回 `404`，状态不允许下载返回 `409`，存储不可用返回 `503`。

React 运营报表工作台仅在 Gateway 模式调用上述接口，并以当前认证会话的 `Authorization` 与 `X-Tenant-Id` 进入 Gateway 作用域；创建导出请求固定为 `OPERATIONAL_SUMMARY_CSV` 和所选日期范围。`READY` 状态点击下载后才请求访问地址，页面不将该地址写入状态持久化、日志或本地存储。

## 基础资料管理

| 方法 | 路径 | 规则 |
| --- | --- | --- |
| GET/POST | `/api/v1/master-data/rooms` | 查询或创建当前机构房间；`includeDisabled=true` 才返回已停用数据。 |
| PATCH | `/api/v1/master-data/rooms/{roomId}` | 使用 `version` 编辑房间。 |
| POST | `/api/v1/master-data/rooms/{roomId}/status` | 使用 `version` 启停房间；停用前房间内不得存在启用床位。 |
| GET/POST | `/api/v1/master-data/beds` | 查询或创建当前机构床位。 |
| PATCH | `/api/v1/master-data/beds/{bedId}` | 使用 `version` 编辑空闲启用床位。 |
| POST | `/api/v1/master-data/beds/{bedId}/status` | 使用 `version` 启停床位；停用前须空闲、已清洁且未关联在院长者。 |
| GET/POST | `/api/v1/master-data/catalogs/{category}` | 查询或创建 `CARE_ITEM`、`ASSESSMENT_TYPE`、`RISK_LEVEL`、`SHIFT` 目录项。 |
| PATCH/POST | `/api/v1/master-data/catalogs/{category}/{itemId}`、`/status` | 更新目录项或使用版本号启停；编码不可修改。 |

以上接口不接收 `tenantId` 或操作者字段；租户和操作者均由 Gateway 注入的可信上下文确定，未授权返回 `403`，跨租户或不存在资源返回 `404`，唯一性/版本/状态冲突返回 `409`。

业务接口经 RuoYi Gateway 以 `/api/v1` 暴露，系统管理接口使用 RuoYi 原生 `/system` 路径；两者都使用 Bearer Token。业务响应包含 `code`、`message`、`data`、`traceId`；RuoYi 系统响应使用 `code`、`msg`、`data`，列表返回 `rows` 与 `total`。服务端从令牌和可信 Gateway 上下文确定租户，客户端不得在请求体传入可覆盖租户的字段。

认证接口位于 Gateway 的 `/auth` 前缀：`POST /auth/login` 接收 `account` 与 `password`，`POST /auth/refresh` 接收刷新令牌。登录响应只返回由 Gateway 签发的会话数据；租户在 `GET /api/v1/me` 和 `GET /api/v1/tenants` 中获取，不能由登录请求指定。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | /me | 当前用户、租户和权限摘要 |
| GET/POST | /residents | 查询或创建长者 |
| GET/PATCH | /residents/{id} | 查看或编辑未归档长者档案 |
| GET | /residents/{id}/360 | 聚合查看当前机构内的健康、评估、入住与护理关联 |
| GET/PATCH | /residents/{id}/health-profile | 查看或受控更新健康档案字段 |
| DELETE | /residents/{id} | 逻辑归档已退住或已结案的长者档案 |
| GET/POST | /residents/{id}/contacts | 查询或新增长者联系人 |
| PATCH/DELETE | /residents/{id}/contacts/{contactId} | 使用联系人版本编辑或删除联系人 |
| GET/POST | /residents/{id}/assessments | 查询或新增长者评估记录 |
| PATCH/DELETE | /residents/{id}/assessments/{assessmentId} | 使用评估版本编辑或删除评估记录 |
| GET/POST | /residents/{id}/attachments | 查询附件元数据或创建待上传附件记录 |
| POST | /residents/{id}/attachments/{attachmentId}/upload-url | 为待上传附件签发短时直传地址 |
| POST | /residents/{id}/attachments/{attachmentId}/complete | 复核私有存储元数据并完成附件上传 |
| POST | /residents/{id}/attachments/{attachmentId}/access-url?inline=true | 为已上传附件签发 5 分钟私有预览或下载地址 |
| GET/POST | /admissions | 查询或创建入住单 |
| GET | /beds?occupancyStatus=AVAILABLE | 查询当前机构床位，可按占用状态筛选 |
| POST | /admissions/{id}/assign-bed | 以版本号分配床位 |
| POST | /admissions/{id}/confirm | 确认入住事务 |
| POST | /admissions/{id}/discharge | 办理退住并解除床位绑定 |
| POST | /admissions/{id}/cancel | 取消未完成入住申请并释放已预留床位 |
| GET/POST | /care-plans | 护理计划查询与创建 |
| PATCH | /care-plans/{id} | 编辑草稿计划，使用计划版本和幂等键 |
| POST | /care-plans/{id}/cancel | 停用草稿或已发布计划，使用计划版本和幂等键 |
| POST | /care-tasks/{id}/complete | 完成任务并创建服务记录 |
| POST | /care-tasks/{id}/exception | 标记异常并创建跟进项 |
| GET/POST | /care-plan-task-templates?planId={id} | 查询或维护指定计划的任务模板 |
| PATCH | /care-plan-task-templates/{id} | 编辑启用模板，使用模板版本和幂等键 |
| POST | /care-plan-task-templates/{id}/deactivate | 停用模板，使用模板版本和幂等键 |
| GET | /care-service-records?residentId=&serviceDate=&executorId= | 在服务端筛选服务记录 |
| POST | /care-task-generation-runs | 按指定服务日期幂等生成每日任务 |
| GET | /care-task-follow-ups | 查询当前机构异常跟进项 |
| POST | /care-task-follow-ups/{id}/resolve | 以版本号结案异常跟进项 |
| GET/POST | /care-shift-handovers | 查询或创建班次交接草稿 |
| POST | /care-shift-handovers/{id}/submit | 以版本号提交班次交接 |
| GET | /devices | 设备概览 |
| POST | /device-alerts/{id}/acknowledge | 确认告警 |
| POST | /device-alerts/{id}/dispatch | 创建处置工单 |
| POST | /ai/care-plan-drafts | 生成需人工确认的计划草稿 |
| POST | /exports | 创建异步导出任务 |

入住、退住与护理的状态变更接口使用 `Idempotency-Key`；需要版本校验的操作冲突时返回 `409` 并附当前版本；权限不足返回 `403`；跨租户资源统一返回 `404`，避免暴露资源存在性。

## 长者档案编辑与归档

`PATCH /api/v1/residents/{id}` 仅更新姓名、性别、出生日期和紧急联系人信息；长者的在院状态与床位关联由入住、换床和退住流程维护，浏览器不得直接修改。`DELETE /api/v1/residents/{id}` 为逻辑归档，仅允许状态为 `DISCHARGED` 或 `DECEASED` 且没有当前床位的长者；在院或待入住长者返回 `409`。查询和归档后的记录均不再通过常规列表和详情接口返回，跨租户资源统一返回 `404`。

联系人、评估与附件元数据均位于 `/api/v1/residents/{id}` 子资源下，服务端先校验该长者属于当前租户。联系人新增请求包含 `name`、`relationshipText`、`phone` 与 `primaryContact`；新主联系人会原子替换旧主联系人。评估请求包含 `assessmentType`、`assessmentDate`、可选 `score`、`riskLevel` 和可选 `note`，评估人由当前成员上下文确定。创建附件记录只提交 `fileName`、`contentType` 和 `byteSize`，返回不含对象键的 `PENDING_UPLOAD` 元数据；随后只能为同一租户、同一长者下的待上传记录请求短时直传地址。浏览器以该地址直接 `PUT` 文件且不附带 Gateway Bearer Token，之后调用完成接口；服务端读取私有存储元数据，实际媒体类型和字节数均匹配时才更新为 `UPLOADED`。重复完成已上传附件安全返回当前记录；状态不符或元数据不匹配返回 `409`，存储未配置或不可用返回 `503`。所有接口均不下发存储密钥、对象键或公开下载地址。

`GET /api/v1/residents/{id}/360` 仅聚合当前租户的基础档案、健康档案、联系人、评估、附件元数据、入住单、护理计划和护理任务；任一跨租户长者统一返回 `404`。`PATCH /api/v1/residents/{id}/health-profile` 接收血型、过敏、慢病、用药说明、照护等级、行动/认知状态、营养/跌倒/压疮/感染风险及照护注意事项，字段长度由服务端校验。`POST /api/v1/residents/{id}/attachments/{attachmentId}/access-url?inline=true` 仅允许 `UPLOADED` 附件，返回短时 `accessUrl` 和到期时间；`inline=false` 请求下载处置。任何状态不符、跨租户资源或未上传附件均不暴露对象键。

入住床位预留和确认入住均必须提供 `Idempotency-Key` 请求头，长度为 1 至 128 个字符。幂等范围为“当前租户 + 当前操作者 + 操作类型 + 请求键”；同一范围内、相同请求指纹的成功重试返回首次结果且不重复写入。若同一键对应不同请求内容，或首个请求仍在处理中，返回 `409`。浏览器不得传递租户字段，也不得生成或读取服务端幂等记录。

## 入住床位预留请求

`POST /api/v1/admissions/{id}/assign-bed` 仅允许当前租户内状态为“待分配”的入住单预留状态为“空闲”且卫生状态为“已准备”的床位。请求体如下：

```json
{
  "bedId": 7001,
  "admissionVersion": 3,
  "bedVersion": 5
}
```

服务端从 Gateway 可信内部身份上下文取得用户与租户，不接收客户端覆盖租户。床位状态与入住单状态通过同一数据库事务及乐观锁原子更新；成功响应及后续查询返回 `reservedUntil`。任一更新未命中时返回 `409`，并回滚已执行的床位预留。跨租户的入住单或床位统一返回 `404`。

## 入住调度查询

`GET /api/v1/admissions` 返回当前租户内的入住单，包含 `residentName`、`roomNo`、`bedNo`、`reservedUntil`、入住单 `version` 与当前床位 `bedVersion`。`GET /api/v1/beds` 返回当前租户床位的 `roomNo`、`roomType`、`bedNo`、`occupancyStatus`、`hygieneStatus` 与 `version`；可选 `occupancyStatus` 仅接受枚举值，例如 `AVAILABLE`。

查询接口不接收 `tenantId`。网关验证身份后向服务传递可信租户上下文，服务端以该上下文执行带 `tenant_id` 的查询。入住调度页面将查询结果中的两个版本号原样带回预留和确认写请求；收到 `409` 后必须丢弃旧版本并刷新列表。

## 确认入住请求

`POST /api/v1/admissions/{id}/confirm` 仅允许当前租户内处于“待确认”状态、`reservedUntil` 尚未到期的入住单提交，且该入住单已绑定一个同租户、卫生已准备、状态为“已预留”的床位。请求体如下：

```json
{
  "admissionVersion": 4,
  "bedVersion": 6
}
```

确认事务依次将床位更新为“已占用”、长者更新为“在院”、入住单更新为“已入住”，并写入 `ADMISSION_CONFIRMED` 审计事件。任一状态或版本校验不通过时返回 `409`，事务回滚且不写入审计事件。

## 取消与超时释放

`POST /api/v1/admissions/{id}/cancel` 使用 `Idempotency-Key`，请求体包含入住单 `admissionVersion`，若当前为待确认入住还必须包含床位 `bedVersion`。待评估或待安排申请直接转为 `CANCELLED`；待确认申请在同一事务中先将同租户且仍为 `RESERVED` 的床位释放，再取消入住申请并写入 `ADMISSION_CANCELLED` 审计事件。已入住、已退住和已取消记录一律返回 `409`。

服务端按 `ADMISSION_RESERVATION_TIMEOUT_MINUTES` 写入预留截止时间，并按 `ADMISSION_RESERVATION_EXPIRY_SCAN_MS` 扫描到期的待确认入住单。释放操作不接受浏览器调用：它仅在服务端事务中执行，恢复申请为待安排、释放床位并写入 `ADMISSION_RESERVATION_EXPIRED` 审计事件。若床位、入住单版本或状态已变化，本次扫描回滚并在下一轮重新读取，不能覆盖人工确认或取消结果。

## 退住请求

`POST /api/v1/admissions/{id}/discharge` 仅允许当前租户内处于“已入住”状态的入住单提交，且当前床位必须仍为“已占用”。请求体如下：

```json
{
  "admissionVersion": 5,
  "bedVersion": 7,
  "dischargeReason": "转至家属照护"
}
```

退住事务使用与确认入住相同的版本校验和 `Idempotency-Key` 范围，依次将床位更新为“清洁中”、长者更新为“已退住”并解除床位绑定、入住单更新为“已退住”并保存退住原因，最后写入 `ADMISSION_DISCHARGED` 审计事件。任一步失败均返回 `409` 并回滚，不写入审计事件；床位完成卫生准备前不得重新分配。

## 护理计划与任务接口（阶段 3 源码）

| 方法 | 路径 | 请求要点 | 结果与规则 |
| --- | --- | --- | --- |
| GET | `/api/v1/care-plans` | 无租户参数 | 返回当前租户的计划版本列表 |
| POST | `/api/v1/care-plans` | `residentId`、`planName`、`frequencyText`、`startDate`、`endDate?` | 创建草稿；责任人使用当前操作者 |
| POST | `/api/v1/care-plans/{id}/publish` | `planVersion` | 仅草稿可发布；替换同一长者的旧发布计划；冲突返回 `409` |
| PATCH | `/api/v1/care-plans/{id}` | `planVersion`、名称、频次、有效日期 | 仅草稿可编辑；版本冲突返回 `409` |
| POST | `/api/v1/care-plans/{id}/cancel` | `planVersion` | 仅草稿或已发布计划可停用；版本冲突返回 `409` |
| GET | `/api/v1/care-tasks` | 无租户参数 | 按计划时间返回当前租户任务 |
| POST | `/api/v1/care-tasks` | `planId`、`taskName`、`scheduledAt`、`assigneeId` | 仅已发布且在有效期内的计划可建任务 |
| POST | `/api/v1/care-tasks/{id}/complete` | `taskVersion`、`resultNote` | 原子更新任务并写入服务记录；冲突返回 `409` |
| POST | `/api/v1/care-tasks/{id}/exception` | `taskVersion`、`exceptionReason` | 将未终态任务标为异常；冲突返回 `409` |
| GET | `/api/v1/care-task-follow-ups?status=OPEN` | 可选状态枚举 | 返回当前租户的异常跟进项 |
| POST | `/api/v1/care-task-follow-ups/{id}/resolve` | `followUpVersion`、`resolutionNote` | 仅开放跟进项可结案；冲突返回 `409` |
| POST | `/api/v1/care-plan-task-templates` | `planId`、`taskName`、`scheduledTime`、`assigneeId` | 为草稿或已发布计划维护启用模板 |
| PATCH | `/api/v1/care-plan-task-templates/{id}` | `templateVersion`、名称、时间、执行人 | 仅启用模板可编辑；版本冲突返回 `409` |
| POST | `/api/v1/care-plan-task-templates/{id}/deactivate` | `templateVersion` | 停用模板，不影响既有任务 |
| GET | `/api/v1/care-service-records` | `residentId?`、`serviceDate?`、`executorId?` | 仅管理权限；在服务端按可选条件筛选 |
| POST | `/api/v1/care-task-generation-runs` | `serviceDate` | 生成该日有效计划任务，重复模板日期不重复写入 |

所有接口均由服务端上下文确定租户，跨租户资源返回不可枚举的 `404`。`assigneeId` 由服务端成员目录校验：目标成员必须属于当前机构且具备对应护理权限，浏览器不能传入租户或权限覆盖字段。所有护理写接口必须传递 `Idempotency-Key`，长度为 1 至 128；作用域为“当前租户 + 当前操作者 + 操作类型 + 请求键”。同键同指纹成功重试重放首次结果，同键不同请求或处理中请求返回 `409`。

联系人和评估的 `PATCH` 请求体包含 `version`，`DELETE` 使用 `?version=`；更新条件始终包含当前租户、长者、子资源和版本。评估若被状态非 `CANCELLED` 的护理计划以 `assessmentId` 引用，删除返回 `409`。创建或更新护理计划可选传入 `assessmentId`，服务端只接受同租户、同长者的评估记录。

前端 Gateway 护理工作台已接入计划创建、编辑、发布和停用，模板创建、编辑和停用，每日生成、任务创建/开始/完成/异常、异常跟进结案、班次交接以及服务记录按长者/日期/成员筛选。写入后的 `409` 或失败均重新读取服务端队列，成员选择来自当前机构成员目录。

## 入住与床位闭环（阶段 5 源码）

`POST /api/v1/admissions/{id}/assessment` 接收 `admissionVersion`、`decision`（`PASSED`/`REJECTED`）和 `conclusion`；`POST /api/v1/admissions/{id}/transfer-bed` 接收目标床位、入住单/原床位/目标床位版本及理由；`POST /api/v1/beds/{id}/complete-cleaning` 接收床位版本与清洁结果；`GET /api/v1/admissions/{id}/audit` 返回最多 50 条当前租户的最小化操作轨迹。评估和调床写接口要求 `Idempotency-Key`，冲突返回 `409`。

护理任务新增 `POST /api/v1/care-tasks/{id}/start`，请求体为 `taskVersion`，要求 `Idempotency-Key` 且仅当前被分配成员可调用；状态不为待执行、版本不符或重复状态转换返回 `409`。现有完成与异常接口保留相同版本、成员和幂等约束。

## 机构成员管理

机构成员接口使用 RuoYi Gateway 的 `/system/tenant-members` 路径。浏览器必须发送当前选择的 `X-Tenant-Id` 头；Gateway 校验该用户在该租户为启用成员后，才向 `ruoyi-system` 注入可信租户上下文。请求体和查询参数不接受 `tenantId`。

| 方法 | 路径 | 请求体 | 规则 |
| --- | --- | --- | --- |
| GET | `/system/tenant-members` | 无 | 返回当前机构成员及其机构角色、部门、数据范围、系统账号与成员状态。 |
| POST | `/system/tenant-members` | `userId`、非空 `roleIds` | 仅可将未删除的 RuoYi 系统账号加入当前机构；角色必须启用；重复加入安全地恢复成员并覆盖当前机构角色。 |
| PUT | `/system/tenant-members/{userId}/roles` | 非空 `roleIds` | 仅替换当前机构内该成员的角色，不修改该账号在其他机构的角色关联。 |
| PUT | `/system/tenant-members/{userId}/status` | `status` 为 `0` 或 `1` | 启用或停用该成员的当前机构资格；停用后 Gateway 拒绝该机构业务请求。 |

上述接口需要 RuoYi `system:user:list` 或 `system:user:edit` 权限，并使用 `@Log` 记录操作。角色菜单和数据范围继续使用既有 `/system/menu/roleMenuTreeselect/{roleId}` 与 `/system/role/dataScope`，其权限判定由 RuoYi 原生角色数据范围规则负责。
## Gateway 路由前缀

所有养老业务服务接口通过 Gateway 的 `/care/**` 路由暴露。浏览器请求路径为 `/care/api/v1/**`，Gateway 去除首段 `/care` 后转发至 `care-service` 的 `/api/v1/**`。认证、RuoYi System 和成员目录仍分别使用 `/auth/**`、`/system/**`。
