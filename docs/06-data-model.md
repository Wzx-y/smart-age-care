# 数据模型

## 通知与统一审计

`care_notification` 保存租户、接收成员（`0` 表示当前机构广播）、分类、优先级、固定标题、资源关联、长者关联、去重键、创建与已读时间。唯一去重键防止逾期扫描反复写入同一提醒。统一审计不复制业务正文：查询层只合并 `care_audit_log`、`care_master_data_audit` 和 `care_export_audit` 的动作与关联 ID；默认保留 2555 天，由云端合规配置确认后清理。

## 运营报表与导出

`care_export_job` 保存租户、导出类型、统计周期、状态、文件名、私有对象键、创建人、完成时间、过期时间和版本；对象键仅供服务端存储适配器使用。`care_export_audit` 只保存租户、导出任务、可选操作者、动作和时间，用于 `CREATED`、`READY`、`FAILED`、`DOWNLOADED`、`EXPIRED` 轨迹。运营报表不创建明细快照，按请求周期对 `care_resident`、`care_bed`、`care_admission`、`care_task`、`care_service_record` 和 `care_task_follow_up` 做实时聚合。

## 基础资料物理表

`care_room` 增加 `nursing_unit`、`enabled`、`version` 和 `updated_at`；`care_bed` 增加 `equipment_summary`、`enabled` 和 `updated_at`。`care_master_data_item` 以 `tenant_id + category + item_code` 唯一保存护理项目、评估类型、风险等级和班次，并保留版本、启停与维护人。`care_master_data_audit` 仅保存租户、操作者、资源类型、资源 ID、动作和时间，不复制业务正文。

## 核心实体

| 表 | 关键字段 | 关联与约束 |
| --- | --- | --- |
| tenants | id, code, name, status, plan | code 唯一；所有业务表的租户根节点 |
| users | id, account, status | 由 RuoYi 用户服务维护 |
| tenant_members | tenant_id, user_id, role_id, data_scope | tenant_id + user_id 唯一 |
| residents | id, tenant_id, name, status, current_bed_id | 在院长者只能关联一个有效床位 |
| resident_health_profiles | resident_id, tenant_id, blood_type, allergy_summary, chronic_conditions, medication_notes, care_level, mobility_status, cognition_status, nutrition_risk, fall_risk, pressure_injury_risk, infection_risk, care_notes | 独立保存健康与照护风险字段；只经受控档案接口更新 |
| rooms | id, tenant_id, building, floor, room_no | tenant_id + room_no 唯一 |
| beds | id, tenant_id, room_id, bed_no, occupancy_status, version | room_id + bed_no 唯一；version 用于并发控制 |
| admissions | id, tenant_id, resident_id, bed_id, reserved_until, status, discharged_at, discharged_by, discharge_reason | 预留有截止时间；确认、退住和超时释放均联动床位 |
| care_plans | id, tenant_id, resident_id, owner_id, status | 计划版本可追溯 |
| care_tasks | id, tenant_id, plan_id, scheduled_at, assignee_id, status | tenant_id + plan_id + scheduled_at 建索引 |
| service_records | id, tenant_id, task_id, executor_id, result | task_id 可建立唯一完成记录 |
| devices | id, tenant_id, device_code, location, status | tenant_id + device_code 唯一 |
| device_alerts | id, tenant_id, device_id, priority, status | 按 status、priority、created_at 建索引 |
| maintenance_orders | id, tenant_id, device_id, alert_id, status | 可关联告警或预防性维护 |
| care_audit_log | id, tenant_id, actor_id, action, target_type, target_id, related_resident_id, related_bed_id | 仅记录操作与关联 ID；tenant_id、target、created_at 组合索引 |
| care_admission_idempotency | tenant_id, actor_id, operation, idempotency_key, request_fingerprint, status, result_* | tenant + actor + operation + key 唯一；仅保存重放所需资源 ID、状态与版本，不保存个人照护数据 |

## 行级隔离

若使用 Supabase 存储业务镜像或附件元数据，所有表启用 RLS。策略要求 JWT 声明中的 `tenant_id` 等于记录的 `tenant_id`，并额外验证成员资格与操作权限。服务角色密钥只在受控服务端使用；浏览器匿名密钥无权绕过策略。

## 数据保留

医疗与照护记录按机构合规期限保留；逻辑删除保留审计轨迹；租户注销先冻结、导出、审批，再执行分批删除和不可逆证据记录。

`care_admission` 的退住记录保存退住时间、执行人和不超过 500 字的原因；床位退住后状态为 `CLEANING`，在卫生准备完成前不可再次分配。床位预留写入 `reserved_until`，到期服务会在同一事务中将床位由 `RESERVED` 释放为 `AVAILABLE`、清空入住单的床位和截止时间并恢复为 `PENDING_ASSIGNMENT`。`care_audit_log` 仅保存 `ADMISSION_DISCHARGED`、`ADMISSION_CANCELLED`、`ADMISSION_RESERVATION_EXPIRED` 的关联 ID，不复制退住原因或个人照护正文。

## 护理计划与任务物理表（阶段 3 源码）

`care_plan` 保存 `tenant_id`、`resident_id`、可选 `assessment_id`、`plan_name`、`frequency_text`、`owner_id`、状态、版本和有效日期；`assessment_id` 仅能引用同租户、同长者的评估记录，避免将风险正文复制到计划中。`tenant_id + resident_id + version` 唯一，保证同一长者的计划版本可追溯。草稿更新、计划停用均以版本号条件更新；状态为 `DRAFT`、`PUBLISHED`、`SUPERSEDED`、`CANCELLED`。`idx_care_plan_tenant_resident_status` 支持按长者和状态查询。

`care_resident_contact` 保存长者联系人、关系、电话、主联系人标识和乐观锁版本；切换主联系人时先在同一事务中取消当前标识再写入新联系人。`care_resident_assessment` 保存评估类型、日期、评分、风险等级、观察说明、评估人和乐观锁版本；评估人始终来自可信的当前成员上下文。`care_resident_attachment` 仅保存文件名、媒体类型、字节数、服务端生成的对象键、上传状态、创建时间与 `uploaded_at`；初始状态为 `PENDING_UPLOAD`，私有存储对象的实际类型和大小均由服务端在完成确认时复核，通过后才更新为 `UPLOADED`。对象键不向浏览器序列化，浏览器不接触服务端密钥。

`care_resident_health_profile` 将血型、过敏、慢病、用药说明、照护等级、行动与认知状态、营养/跌倒/压疮/感染风险以及照护注意事项与基础身份资料分离；更新人和更新时间可追溯。附件下载或预览只在已上传状态下由服务端签发 5 分钟访问地址，地址和对象键都不进入持久化的浏览器状态。

`care_task` 保存计划、由计划派生的长者、计划执行时间、执行人、任务状态、异常说明和乐观锁版本。`idx_care_task_tenant_schedule_status` 用于当前机构的班次任务队列，`idx_care_task_tenant_resident` 用于长者 360 视图。`care_service_record` 以 `task_id` 唯一约束保证一个任务只能有一条完成服务记录，并索引机构完成时间。

`care_task_follow_up` 以 `task_id` 唯一关联异常任务，保存长者、开放/已结案状态、异常说明、结案说明、创建/结案人和版本；按机构、状态、创建时间及长者建立索引。`care_write_idempotency` 以“租户 + 操作者 + 操作 + 请求键”唯一，保存 SHA-256 指纹和最小化的结果资源引用，支持计划、任务与异常跟进项的安全重放。

`care_plan_task_template` 保存计划内的任务名称、计划时间、执行人、启用状态和乐观锁版本；模板更新或停用均使用版本条件更新。`care_task.origin_template_id + service_date` 在租户内唯一，保证自动生成幂等。`care_service_record` 查询通过关联任务在服务端按长者、服务日期和执行成员筛选。`care_task_generation_run` 记录生成日期、实际新增任务数和操作者，作为调度审计依据。

MySQL 核心业务库依靠所有 SQL 的 `tenant_id` 条件隔离；若将附件元数据或业务镜像投射至 Supabase，则仍须以 JWT 的 `tenant_id` 启用 RLS，不能因本地 MySQL 查询已有条件而省略策略。

## 入住与床位闭环（阶段 5 源码）

`care_admission_assessment` 保存入住单的评估决策、结论、评估人和时间，可保留未通过后的重新评估历史。`care_admission_bed_transfer` 保存原/目标床位、调床理由、执行人和时间。`care_bed_cleaning_record` 保存清洁结果、执行人和完成时间；三张表均包含 `tenant_id` 并按租户与关联实体建立索引。

## 机构成员目录与角色

`care_tenant_member` 的主键为 `tenant_id + user_id`，保存成员在该机构的启停状态和维护时间。`care_tenant_member_role` 的主键为 `tenant_id + user_id + role_id`，只引用已启用的 RuoYi 角色；它把角色分配限定到单个机构，不在业务授权查询中使用全局 `sys_user_role` 作为替代来源。成员查询联接 `sys_user`、`sys_dept` 和租户角色以展示账号、部门、角色名与数据范围，但不会复制口令或令牌字段。
