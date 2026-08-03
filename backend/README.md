# 智护云 Care 后端

该目录承载养老领域服务，不复制或修改 RuoYi Cloud 的平台源码。生产接入时，RuoYi Cloud 独立负责 Gateway、认证、租户、角色、权限、审计和服务注册；本目录的服务仅接收 Gateway 已验证并签名传递的用户与租户上下文。

## 模块

- `care-service`：长者档案、入住单、房间、床位、护理计划、任务和交接。
- `device-service`：设备产品、设备、遥测标准化、设备影子、告警、工单和维护。

## 技术约束

- Java 21、Spring Boot 3、Maven、MySQL 8。
- 服务间身份由 RuoYi Gateway 传递；浏览器不得直接调用内部服务。
- 每个业务表必须包含 `tenant_id`，每个查询与写入必须以可信租户上下文为范围。
- 设备数据经 EMQX/协议适配进入 `device-service`；设备不得直接访问 MySQL 或 `care-service`。

本机不运行 Maven、Java 或数据库。首次构建、依赖下载、迁移和测试只在 Codespaces 或 GitHub Actions 进行。
